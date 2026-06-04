pipeline {
    agent any
    
    tools {
        nodejs 'NodeJS_20' 
    }
    
    environment {
        JIRA_API_TOKEN          = credentials('JIRA_API_TOKEN')
        JIRA_DOMAIN             = 'caongoctanvo.atlassian.net'
        JIRA_EMAIL              = 'caongoctanvo@gmail.com'
        JIRA_PROJECT_KEY        = 'TAP'
        JIRA_SERVICE_ISSUE_TYPE = 'Epic'
        JIRA_AUTOMATION_ISSUE_TYPE = 'Task'
        JIRA_BUG_ISSUE_TYPE     = 'Subbug'
        POSTMAN_COLLECTION_ID   = '55110231-c5612bea-0c70-4885-8563-a2a269fd1756' 
        API_BASE_URL            = 'http://gateway:8080/api' 
    }
    
    triggers {
        // Đúng 00:00 (Múi giờ UTC) chạy Full
        cron('0 0 * * *')
        // Quét code mới mỗi 2 tiếng
        pollSCM('H/2 * * * *')
    }
    
    stages {
        stage('1. Kéo Code Từ Git') {
            steps {
                echo '📥 Đang kéo phiên bản code mới nhất từ kho lưu trữ...'
                checkout scm
            }
        }
        
        stage('2. Cài Đặt Môi Trường') {
            steps {
                echo '📦 Đang cài đặt các thư viện Node.js cần thiết...'
                dir('backend/test') {
                    sh 'npm install newman dotenv axios'
                }
            }
        }
        
        stage('3. Chạy Automation Test & Đồng Bộ Jira') {
            steps {
                dir('backend/test') {
                    script {
                        // Kiểm tra xem Jenkins đang chạy vì đến giờ (00:00) hay vì có người Push Code
                        def isTimer = currentBuild.getBuildCauses('hudson.triggers.TimerTrigger$TimerTriggerCause')
                        
                        if (isTimer) {
                            // TRƯỜNG HỢP 1: CHẠY THEO GIỜ ĐÃ HẸN (00:00)
                            echo '🕒 Chạy theo lịch 00:00 -> Đang test TOÀN BỘ hệ thống...'
                            sh 'node jira_automation.js'
                        } else {
                            // TRƯỜNG HỢP 2: CÓ NGƯỜI PUSH CODE LÊN
                            echo '💻 Phát hiện có code mới Push lên -> Đang phân tích Commit Message...'
                            
                            // Đọc lời nhắn commit cuối cùng của Dev
                            def commitMsg = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                            echo "Lời nhắn commit: ${commitMsg}"
                            
                            // Quét xem Dev đang muốn test service nào
                            if (commitMsg.contains('[station-service]')) {
                                echo '🚀 Bắt đầu test riêng lẻ: station-service'
                                sh 'node jira_automation.js "station-service"'
                            } 
                            else if (commitMsg.contains('[users-service]')) {
                                echo '🚀 Bắt đầu test riêng lẻ: users-service'
                                sh 'node jira_automation.js "users-service"'
                            }
                            else if (commitMsg.contains('[vehicle-service]')) {
                                echo '🚀 Bắt đầu test riêng lẻ: vehicle-service'
                                sh 'node jira_automation.js "vehicle-service"'
                            }
                            // Thêm các service khác của nhóm vào đây nếu cần...
                            else {
                                // Nếu Dev quên không ghi chữ [tên-service] thì tự động test Full cho an toàn
                                echo '⚠️ Không tìm thấy [tên-service] trong commit -> Test TOÀN BỘ hệ thống.'
                                sh 'node jira_automation.js'
                            }
                        }
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo '✅ Tuyệt vời! Tất cả các ca kiểm thử đều Pass. Không có Bug mới.'
        }
        failure {
            echo '⚠️ Phát hiện lỗi (Bug). Hệ thống đã tự động đẩy/cập nhật thông tin lên thẻ Jira cho Dev.'
        }
        always {
            echo '🏁 Tiến trình CI/CD hoàn tất, tự động dọn dẹp môi trường.'
        }
    }
}