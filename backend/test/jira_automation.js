require("dotenv").config();
const fs = require("fs");
const axios = require("axios");
const crypto = require("crypto");
const { execSync } = require("child_process");

// ==================== 1. CẤU HÌNH THÔNG TIN TỪ .ENV ====================
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
// Kiểm tra điều kiện bảo mật bắt buộc
if (!JIRA_API_TOKEN) {
  console.error(
    "❌ Lỗi: Thiếu cấu hình bí mật JIRA_API_TOKEN trong file .env!",
  );
  process.exit(1);
}

// Khởi tạo bộ sinh cấu hình Header Basic Auth cho Jira
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

// Xây dựng sơ đồ đường dẫn request từ collection gốc để map tên thư mục chính xác
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

function getPathFromExecution(execution, requestPathMap, serviceName) {
  const itemId = execution.item?.id;
  const itemName = execution.item?.name;
  if (itemId && requestPathMap[itemId]) return requestPathMap[itemId];
  if (itemName && requestPathMap[itemName]) return requestPathMap[itemName];
  return [serviceName, "Unknown Flow", itemName || "Unknown Request"];
}

function getFlowNameFromPath(path, serviceName) {
  const serviceIndex = path.findIndex((part) => part === serviceName);
  if (serviceIndex >= 0) {
    const foldersAfterService = path.slice(serviceIndex + 1, -1);
    if (foldersAfterService.length > 0)
      return foldersAfterService[foldersAfterService.length - 1];
  }
  return path.length >= 2 ? path[path.length - 2] : `${serviceName} Flow`;
}

// Băm mã vân tay độc nhất (MD5 Fingerprint) chống trùng lắp lỗi
function createFingerprint(serviceName, failure) {
  const raw = `${serviceName}|${failure.flowName}|${failure.requestName}|${failure.method}|${failure.testName}`;
  const hash = crypto
    .createHash("md5")
    .update(raw)
    .digest("hex")
    .substring(0, 12);
  return { raw, label: `fp-${hash}` };
}

// Trích xuất toàn bộ ca kiểm thử thất bại từ file báo cáo chuẩn
function parseFailuresFromReport(report) {
  const failures = [];
  const requestPathMap = buildRequestPathMap(report.collection);
  const executions = report.run?.executions || [];

  for (const execution of executions) {
    // 1. Lấy mảng cấu trúc thư mục của request hiện tại
    let path = [];
    const itemId = execution.item?.id;
    const itemName = execution.item?.name;

    if (itemId && requestPathMap[itemId]) path = requestPathMap[itemId];
    else if (itemName && requestPathMap[itemName])
      path = requestPathMap[itemName];
    else
      path = ["Unknown-Service", "Unknown Flow", itemName || "Unknown Request"];

    // 2. LẤY EPIC: Tên service LUÔN LÀ thư mục ngoài cùng (Ví dụ: users-service)
    const serviceName = path[0];

    // 3. LẤY TASK: Tên Flow (Luồng) là thư mục nằm kế cuối
    let flowName = "Unknown Flow";
    if (path.length >= 3) {
      flowName = path[path.length - 2];
    } else if (path.length === 2) {
      flowName = `${serviceName} Flow`;
    }

    const requestName =
      execution.item?.name || path[path.length - 1] || "Unknown request";
    const method = execution.request?.method || "UNKNOWN";
    const url = execution.request?.url?.raw || "Unknown URL";
    const statusCode = execution.response?.code || "No response";
    const responseTime = execution.response?.responseTime || "N/A";

    // Quét lỗi Assertions
    (execution.assertions || []).forEach((assertion) => {
      if (assertion.error) {
        failures.push({
          serviceName, // Đính kèm serviceName vào từng lỗi để lát phân loại
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

    // Quét lỗi Request bị sập trước khi assert
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

// ==================== 3. CÁC HÀM GIAO TIẾP VỚI JIRA API (NÂNG CẤP KIỂM TRÙNG) ====================
async function searchIssueBySummary(summary, labels = []) {
  const labelJql = labels.map((label) => `AND labels = ${label}`).join(" ");
  const escapedSummary = summary.replace(/"/g, '\\"');
  const jql = `project = ${JIRA_PROJECT} AND summary ~ "${escapedSummary}" ${labelJql} AND statusCategory != Done`;

  try {
    const res = await jira.get("/rest/api/3/search/jql", {
      params: { jql, maxResults: 1, fields: "summary,status" },
    });
    return res.data.issues?.[0] || null;
  } catch {
    return null;
  }
}

async function searchIssueByFingerprint(fingerprintLabel, automationIssueKey) {
  const jql = `project = ${JIRA_PROJECT} AND parent = ${automationIssueKey} AND labels = api-test AND labels = ${fingerprintLabel} AND statusCategory != Done`;
  try {
    const res = await jira.get("/rest/api/3/search/jql", {
      params: { jql, maxResults: 1, fields: "summary,status" },
    });
    return res.data.issues?.[0] || null;
  } catch {
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

  const res = await jira.post("/rest/api/3/issue", { fields });
  return res.data;
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

// Kiểm tra hoặc tạo mới Phân hệ Epic (Cấp 1)
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

// Kiểm tra hoặc TÁI SỬ DỤNG Task quản lý Luồng (Cấp 2) - TRÁNH DUPLICATE TASK CHA
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

// Tạo thông tin Bug con chi tiết (Cấp 3)
async function createBugIssue(
  failure,
  fingerprint,
  automationIssueKey,
  serviceName,
) {
  const serviceLabel = safeLabel(serviceName);
  const flowLabel = safeLabel(failure.flowName);
  const summary =
    `[Bug] Failed Request: ${failure.requestName} - ${failure.testName}`.substring(
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

// ==================== 4. TIẾN TRÌNH THỰC THI (MAIN PIPELINE) ====================
function runNewmanTest(folderName) {
  console.log(`\n==================================================`);
  if (folderName) {
    console.log(`🚀 BẮT ĐẦU CHẠY NEWMAN TEST CHO THƯ MỤC: [${folderName}]`);
  } else {
    console.log(`🚀 BẮT ĐẦU CHẠY NEWMAN TEST CHO TOÀN BỘ HỆ THỐNG`);
  }
  console.log(`==================================================`);

  // Tạo thư mục chứa report nếu chưa có
  if (!fs.existsSync("newman")) fs.mkdirSync("newman");

  const collectionFile = "report_collection.json";

  // Khởi tạo câu lệnh mặc định (Chạy toàn bộ collection)
  let command = `npx newman run "${collectionFile}" --env-var "url=${API_BASE_URL}" -r json --reporter-json-export ${reportFile}`;

  // Nếu có truyền tên folder thì mới thêm cờ --folder vào lệnh
  if (folderName) {
    command = `npx newman run "${collectionFile}" --folder "${folderName}" --env-var "url=${API_BASE_URL}" -r json --reporter-json-export ${reportFile}`;
  }

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
    const failures = parseFailuresFromReport(report); // KHÔNG truyền targetFolder nữa

    console.log(`\nTổng số lỗi Assertions ghi nhận được: ${failures.length}`);

    if (failures.length === 0) {
      console.log(
        "🎉 Tuyệt vời! Không có lỗi nào. Bỏ qua bước đồng bộ dữ liệu Jira.",
      );
      return;
    }

    // BƯỚC MỚI: Nhóm lỗi theo TỪNG SERVICE, sau đó nhóm tiếp theo TỪNG FLOW
    const groupedData = {};
    failures.forEach((f) => {
      if (!groupedData[f.serviceName]) groupedData[f.serviceName] = {};
      if (!groupedData[f.serviceName][f.flowName])
        groupedData[f.serviceName][f.flowName] = [];
      groupedData[f.serviceName][f.flowName].push(f);
    });

    // Lặp qua từng Service (Tạo Epic tương ứng)
    for (const [serviceName, flows] of Object.entries(groupedData)) {
      console.log(`\n==================================================`);
      console.log(`🔍 Đang xử lý phân hệ Epic [${serviceName}] trên Jira...`);

      const serviceIssueKey = await getOrCreateServiceIssue(serviceName);
      if (!serviceIssueKey) continue;

      // Lặp qua từng Flow trong Service đó (Tạo Task)
      for (const [flowName, flowFailures] of Object.entries(flows)) {
        console.log(`  -> Flow: ${flowName} (${flowFailures.length} lỗi)`);

        const automationIssueKey = await getOrCreateAutomationIssue(
          serviceIssueKey,
          flowName,
          serviceName,
        );
        if (!automationIssueKey) continue;

        // Lặp qua từng Bug trong Flow đó (Tạo Subbug)
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
              `Retest: still failed - ${timeString}`,
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

// ==================== HÀM KÍCH HOẠT CHÍNH ====================
async function main() {
  const targetFolder = process.argv[2];

  if (targetFolder) {
    console.log(`\n🔍 Đang chuẩn bị chạy test cho luồng: [${targetFolder}]...`);
    runNewmanTest(targetFolder);
  } else {
    console.log(`\n🔍 Đang chuẩn bị chạy test cho TOÀN BỘ collection...`);
    runNewmanTest(); // Chạy toàn bộ
  }

  // Đồng bộ lỗi tự động bóc tách thành nhiều Epic dựa vào tên thư mục
  await startSyncBugsToJira();
}

main();
