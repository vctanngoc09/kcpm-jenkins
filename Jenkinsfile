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
    
    // THÊM ĐOẠN NÀY ĐỂ HẸN LỊCH CHẠY TỰ ĐỘNG
    triggers {
        cron('10 5 * * 2')
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
                echo '🚀 Bắt đầu chạy Newman Test và kiểm tra lỗi...'
                dir('backend/test') {
                    // Bạn nhớ đổi tên thư mục trùng với tên Folder trong Postman nha
                    sh 'node jira_automation.js'
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