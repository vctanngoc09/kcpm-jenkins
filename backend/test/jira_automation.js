require("dotenv").config();
const fs = require("fs");
const axios = require("axios");
const crypto = require("crypto");
const { execSync } = require("child_process");

// ==================== 1. CẤU HÌNH THÔNG TIN TỪ .ENV ====================
const {
  JIRA_DOMAIN,
  JIRA_EMAIL,
  JIRA_API_TOKEN,
  JIRA_PROJECT_KEY,
  JIRA_SERVICE_ISSUE_TYPE,
  JIRA_AUTOMATION_ISSUE_TYPE,
  JIRA_BUG_ISSUE_TYPE,
  API_BASE_URL,
} = process.env;

const JIRA_PROJECT = JIRA_PROJECT_KEY || "SCRUM";
const reportFile = "newman/report.json";

if (!JIRA_API_TOKEN) {
  console.error(
    "❌ Lỗi: Thiếu cấu hình bí mật JIRA_API_TOKEN trong file .env!",
  );
  process.exit(1);
}

const authBase64 = Buffer.from(`${JIRA_EMAIL}:${JIRA_API_TOKEN}`).toString(
  "base64",
);
const jira = axios.create({
  baseURL: `https://${JIRA_DOMAIN}`,
  headers: {
    Authorization: `Basic ${authBase64}`,
    Accept: "application/json",
    "Content-Type": "application/json",
  },
});

// ==================== 2. CÁC HÀM TRỢ NĂNG XỬ LÝ FORMAT DỮ LIỆU ====================
function toADF(text) {
  return {
    type: "doc",
    version: 1,
    content: [{ type: "paragraph", content: [{ type: "text", text }] }],
  };
}

function safeLabel(text) {
  return String(text)
    .toLowerCase()
    .replace(/[^a-z0-9-_]/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "")
    .substring(0, 40);
}

function buildRequestPathMap(collection) {
  const map = {};
  function walk(items = [], path = []) {
    for (const item of items) {
      if (item.item) {
        walk(item.item, [...path, item.name]);
      } else {
        if (item.id) map[item.id] = [...path, item.name];
        if (item.name) map[item.name] = [...path, item.name];
      }
    }
  }
  walk(collection?.item || []);
  return map;
}

function createFingerprint(serviceName, failure) {
  const raw = `${serviceName}|${failure.flowName}|${failure.requestName}|${failure.method}|${failure.testName}`;
  const hash = crypto
    .createHash("md5")
    .update(raw)
    .digest("hex")
    .substring(0, 12);
  return { raw, label: `fp-${hash}` };
}

function parseFailuresFromReport(report) {
  const failures = [];
  const requestPathMap = buildRequestPathMap(report.collection);
  const executions = report.run?.executions || [];

  for (const execution of executions) {
    let path = [];
    const itemId = execution.item?.id;
    const itemName = execution.item?.name;

    if (itemId && requestPathMap[itemId]) path = requestPathMap[itemId];
    else if (itemName && requestPathMap[itemName])
      path = requestPathMap[itemName];
    else
      path = ["Unknown-Service", "Unknown Flow", itemName || "Unknown Request"];

    const serviceName = path[0];
    let flowName = "Unknown Flow";
    if (path.length >= 3) flowName = path[path.length - 2];
    else if (path.length === 2) flowName = `${serviceName} Flow`;

    const requestName =
      execution.item?.name || path[path.length - 1] || "Unknown request";
    const method = execution.request?.method || "UNKNOWN";
    const url = execution.request?.url?.raw || "Unknown URL";
    const statusCode = execution.response?.code || "No response";
    const responseTime = execution.response?.responseTime || "N/A";

    (execution.assertions || []).forEach((assertion) => {
      if (assertion.error) {
        failures.push({
          serviceName,
          flowName,
          requestName,
          method,
          url,
          statusCode,
          responseTime,
          testName: assertion.assertion || "Unknown test",
          errorMessage: assertion.error.message || "Unknown error",
        });
      }
    });

    if (execution.requestError) {
      failures.push({
        serviceName,
        flowName,
        requestName,
        method,
        url,
        statusCode: "Request Error",
        responseTime,
        testName: "Request failed before assertion",
        errorMessage: execution.requestError.message || "Unknown request error",
      });
    }
  }
  return failures;
}

// ==================== 3. GIAO TIẾP VỚI JIRA API ====================
async function searchIssueBySummary(summary, labels = []) {
  // SỬA LỖI 1: Bọc ngoặc kép cho labels để tránh lỗi JQL khi có dấu gạch ngang
  const labelJql = labels.map((label) => `labels = "${label}"`).join(" AND ");
  const escapedSummary = summary.replace(/"/g, '\\"');
  const jql = `project = "${JIRA_PROJECT}" AND summary ~ "${escapedSummary}" AND ${labelJql} AND statusCategory != Done`;

  try {
    // SỬA LỖI 2: Đổi endpoint về chuẩn /rest/api/3/search
    const res = await jira.get("/rest/api/3/search", {
      params: { jql, maxResults: 1, fields: "summary,status" },
    });
    return res.data.issues?.[0] || null;
  } catch (err) {
    console.error(
      `\n⚠️ Cảnh báo Search JQL:`,
      JSON.stringify(err.response?.data || err.message),
    );
    return null;
  }
}

async function searchIssueByFingerprint(fingerprintLabel, automationIssueKey) {
  const jql = `project = "${JIRA_PROJECT}" AND parent = "${automationIssueKey}" AND labels = "api-test" AND labels = "${fingerprintLabel}" AND statusCategory != Done`;
  try {
    const res = await jira.get("/rest/api/3/search", {
      params: { jql, maxResults: 1, fields: "summary,status" },
    });
    return res.data.issues?.[0] || null;
  } catch (err) {
    console.error(
      `\n⚠️ Cảnh báo Search Fingerprint:`,
      JSON.stringify(err.response?.data || err.message),
    );
    return null;
  }
}

async function createIssue({
  summary,
  description,
  issueType,
  labels,
  parentKey,
}) {
  const fields = {
    project: { key: JIRA_PROJECT },
    summary,
    description: toADF(description),
    issuetype: { name: issueType },
    labels,
  };
  if (parentKey) fields.parent = { key: parentKey };

  try {
    const res = await jira.post("/rest/api/3/issue", { fields });
    return res.data;
  } catch (err) {
    // SỬA LỖI 3: In ra lỗi thực sự từ Jira để dễ dàng debug
    const jiraError =
      err.response?.data?.errors || err.response?.data || err.message;
    console.error(
      `\n❌ Lỗi JIRA từ chối tạo Issue [${issueType}]:`,
      JSON.stringify(jiraError, null, 2),
    );
    throw new Error(JSON.stringify(jiraError));
  }
}

async function addRetestComment(issueKey, text) {
  try {
    await jira.post(`/rest/api/3/issue/${issueKey}/comment`, {
      body: toADF(text),
    });
  } catch (err) {
    console.error(`❌ Không thể thêm comment vào ${issueKey}:`, err.message);
  }
}

async function getOrCreateServiceIssue(serviceName) {
  const serviceLabel = safeLabel(serviceName);
  const summary = serviceName;
  const existing = await searchIssueBySummary(summary, [
    "api-test-service",
    serviceLabel,
  ]);

  if (existing) {
    console.log(`   -> 🎉 Tìm thấy Epic sẵn có: ${existing.key}`);
    return existing.key;
  }
  const issue = await createIssue({
    summary,
    description: `Epic tổng quản lý tự động kiểm thử phân hệ: ${serviceName}.`,
    issueType: JIRA_SERVICE_ISSUE_TYPE || "Epic",
    labels: ["api-test-service", serviceLabel],
  });
  console.log(`   -> 🚀 Tạo thành công Epic lớn mới: ${issue.key}`);
  return issue.key;
}

async function getOrCreateAutomationIssue(
  serviceIssueKey,
  flowName,
  serviceName,
) {
  const serviceLabel = safeLabel(serviceName);
  const flowLabel = safeLabel(flowName);
  const summary = `[Automation Test] Flow: ${flowName}`;
  const existing = await searchIssueBySummary(summary, [
    "api-test-run",
    serviceLabel,
    flowLabel,
  ]);

  if (existing) {
    console.log(
      `   🔥 Tái sử dụng Task quản lý luồng hiện tại: ${existing.key}`,
    );
    return existing.key;
  }
  const issue = await createIssue({
    summary,
    description: `Automation test task for service: ${serviceName}\nTest flow: ${flowName}`,
    issueType: JIRA_AUTOMATION_ISSUE_TYPE || "Task",
    labels: ["api-test-run", serviceLabel, flowLabel],
    parentKey: serviceIssueKey,
  });
  console.log(`   🔥 Tạo mới Task quản lý lỗi luồng: ${issue.key}`);
  return issue.key;
}

async function createBugIssue(
  failure,
  fingerprint,
  automationIssueKey,
  serviceName,
) {
  const serviceLabel = safeLabel(serviceName);
  const flowLabel = safeLabel(failure.flowName);
  const summary =
    `[Bug] Lỗi Request: ${failure.requestName} - ${failure.testName}`.substring(
      0,
      200,
    );
  const description = `Service: ${serviceName}\nTest flow: ${failure.flowName}\n\nRequest: ${failure.requestName}\nMethod: ${failure.method}\nURL: ${failure.url}\nStatus code: ${failure.statusCode}\nResponse time: ${failure.responseTime} ms\n\nFailed test:\n${failure.testName}\n\nError:\n${failure.errorMessage}\n\nFingerprint:\n${fingerprint.raw}`;

  const issue = await createIssue({
    summary,
    description,
    issueType: JIRA_BUG_ISSUE_TYPE || "Subbug",
    labels: ["api-test", "newman", serviceLabel, flowLabel, fingerprint.label],
    parentKey: automationIssueKey,
  });
  return issue;
}

// ==================== 4. TIẾN TRÌNH THỰC THI CHÍNH ====================
function runNewmanTest(folderName) {
  console.log(`\n==================================================`);
  if (folderName)
    console.log(`🚀 BẮT ĐẦU CHẠY NEWMAN TEST CHO THƯ MỤC: [${folderName}]`);
  else console.log(`🚀 BẮT ĐẦU CHẠY NEWMAN TEST CHO TOÀN BỘ HỆ THỐNG`);
  console.log(`==================================================`);

  if (!fs.existsSync("newman")) fs.mkdirSync("newman");
  const collectionFile = "report_collection.json";
  let command = `npx newman run "${collectionFile}" --env-var "url=${API_BASE_URL}" -r json --reporter-json-export ${reportFile}`;
  if (folderName)
    command = `npx newman run "${collectionFile}" --folder "${folderName}" --env-var "url=${API_BASE_URL}" -r json --reporter-json-export ${reportFile}`;

  try {
    execSync(command, { stdio: "inherit" });
    console.log("✅ Tuyệt vời! API Test Pass 100%! Không có lỗi nào.");
  } catch (error) {
    console.log(
      "\n⚠️ Phát hiện ca kiểm thử thất bại. Đang tiến hành phân tích mã vân tay lỗi...\n",
    );
  }
}

async function startSyncBugsToJira() {
  try {
    if (!fs.existsSync(reportFile)) {
      console.error(`❌ Báo cáo kết quả test ${reportFile} không tồn tại.`);
      return;
    }

    const report = JSON.parse(fs.readFileSync(reportFile, "utf8"));
    const failures = parseFailuresFromReport(report);

    console.log(`\nTổng số lỗi Assertions ghi nhận được: ${failures.length}`);

    if (failures.length === 0) {
      console.log(
        "🎉 Tuyệt vời! Không có lỗi nào. Bỏ qua bước đồng bộ dữ liệu Jira.",
      );
      return;
    }

    const groupedData = {};
    failures.forEach((f) => {
      if (!groupedData[f.serviceName]) groupedData[f.serviceName] = {};
      if (!groupedData[f.serviceName][f.flowName])
        groupedData[f.serviceName][f.flowName] = [];
      groupedData[f.serviceName][f.flowName].push(f);
    });

    for (const [serviceName, flows] of Object.entries(groupedData)) {
      console.log(`\n==================================================`);
      console.log(`🔍 Đang xử lý phân hệ Epic [${serviceName}] trên Jira...`);

      const serviceIssueKey = await getOrCreateServiceIssue(serviceName);
      if (!serviceIssueKey) continue;

      for (const [flowName, flowFailures] of Object.entries(flows)) {
        console.log(`  -> Flow: ${flowName} (${flowFailures.length} lỗi)`);

        const automationIssueKey = await getOrCreateAutomationIssue(
          serviceIssueKey,
          flowName,
          serviceName,
        );
        if (!automationIssueKey) continue;

        for (const failure of flowFailures) {
          const fingerprint = createFingerprint(serviceName, failure);
          const existingBug = await searchIssueByFingerprint(
            fingerprint.label,
            automationIssueKey,
          );

          if (existingBug) {
            console.log(
              `      ↳ ⚠️ Bug trùng lặp: ${existingBug.key}. Đang cập nhật comment...`,
            );
            const timeString = new Date().toLocaleString("vi-VN", {
              timeZone: "Asia/Ho_Chi_Minh",
              hour12: false,
            });
            await addRetestComment(
              existingBug.key,
              `Retest: vẫn thất bại vào lúc - ${timeString}`,
            );
            continue;
          }

          const newBug = await createBugIssue(
            failure,
            fingerprint,
            automationIssueKey,
            serviceName,
          );
          console.log(`      ↳ ✅ Tạo Bug mới thành công: ${newBug.key}`);
        }
      }
    }
    console.log("\n🏁 HOÀN TẤT ĐỒNG BỘ LỖI CHỐNG TRÙNG LÊN JIRA THÀNH CÔNG!");
  } catch (error) {
    console.error("❌ Trục trặc hệ thống xử lý đồng bộ:", error.message);
  }
}

async function main() {
  const targetFolder = process.argv[2];
  if (targetFolder) {
    console.log(`\n🔍 Đang chuẩn bị chạy test cho luồng: [${targetFolder}]...`);
    runNewmanTest(targetFolder);
  } else {
    console.log(`\n🔍 Đang chuẩn bị chạy test cho TOÀN BỘ collection...`);
    runNewmanTest();
  }
  await startSyncBugsToJira();
}

main();
