-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(100),
    phone VARCHAR(20)
);

-- 创建手势分析记录表
CREATE TABLE IF NOT EXISTS gesture_analysis_records (
    analysis_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    gesture_data TEXT NOT NULL,
    content_image LONGTEXT,
    explanation TEXT,
    confidence DECIMAL(3,2),
    created_at TIMESTAMP NOT NULL,
    processing_time_ms BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(500),
    api_response TEXT,
    subject_area VARCHAR(50)
);

-- 创建实验会话表
CREATE TABLE IF NOT EXISTS experiment_sessions (
    session_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    experiment_type VARCHAR(100) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    config_data TEXT,
    report_data LONGTEXT,
    last_updated TIMESTAMP,
    experiment_name VARCHAR(200),
    description VARCHAR(500),
    total_data_points INTEGER DEFAULT 0,
    alert_count INTEGER DEFAULT 0,
    duration_minutes INTEGER,
    websocket_connection_id VARCHAR(100)
);

-- 创建API调用日志表
CREATE TABLE IF NOT EXISTS api_call_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_name VARCHAR(100) NOT NULL,
    request_data TEXT,
    response_data TEXT,
    status_code INTEGER,
    response_time_ms BIGINT NOT NULL,
    call_time TIMESTAMP NOT NULL,
    success BOOLEAN NOT NULL,
    error_message VARCHAR(1000),
    user_id VARCHAR(50),
    request_id VARCHAR(100),
    endpoint VARCHAR(500),
    http_method VARCHAR(10),
    request_size_bytes BIGINT,
    response_size_bytes BIGINT,
    retry_count INTEGER DEFAULT 0
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_user_active ON users(active);

CREATE INDEX IF NOT EXISTS idx_gesture_user_id ON gesture_analysis_records(user_id);
CREATE INDEX IF NOT EXISTS idx_gesture_created_at ON gesture_analysis_records(created_at);
CREATE INDEX IF NOT EXISTS idx_gesture_confidence ON gesture_analysis_records(confidence);

CREATE INDEX IF NOT EXISTS idx_experiment_user_id ON experiment_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_experiment_status ON experiment_sessions(status);
CREATE INDEX IF NOT EXISTS idx_experiment_start_time ON experiment_sessions(start_time);
CREATE INDEX IF NOT EXISTS idx_experiment_type ON experiment_sessions(experiment_type);

CREATE INDEX IF NOT EXISTS idx_api_name ON api_call_logs(api_name);
CREATE INDEX IF NOT EXISTS idx_api_call_time ON api_call_logs(call_time);
CREATE INDEX IF NOT EXISTS idx_api_success ON api_call_logs(success);
CREATE INDEX IF NOT EXISTS idx_api_status_code ON api_call_logs(status_code);
CREATE INDEX IF NOT EXISTS idx_api_response_time ON api_call_logs(response_time_ms);