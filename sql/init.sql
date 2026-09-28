-- =========================================================
-- 1. 创建数据库
-- =========================================================

CREATE DATABASE IF NOT EXISTS intelligent_customer
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE intelligent_customer;


-- =========================================================
-- 2. 客户表
-- =========================================================

CREATE TABLE IF NOT EXISTS customers (
    id              CHAR(36) NOT NULL COMMENT '客户UUID',
    username        VARCHAR(100) NOT NULL COMMENT '登录用户名',
    password_hash   VARCHAR(255) NOT NULL COMMENT 'BCrypt密码',
    name            VARCHAR(100) NOT NULL COMMENT '客户姓名',
    email           VARCHAR(255) NULL COMMENT '邮箱',
    phone           VARCHAR(32) NULL COMMENT '手机号',
    avatar          VARCHAR(500) NULL COMMENT '头像地址',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
    COMMENT 'ACTIVE或DISABLED',
    last_login      DATETIME(3) NULL COMMENT '最后登录时间',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_customers_username (username),
    UNIQUE KEY uk_customers_email (email),
    KEY idx_customers_phone (phone),
    KEY idx_customers_status (status),

    -- check 约束（constratint是约束名，check为业务判断）
    CONSTRAINT chk_customers_status
    CHECK (status IN ('ACTIVE', 'DISABLED'))
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_0900_ai_ci
    COMMENT='客户表';


-- =========================================================
-- 3. 客服人员表
-- =========================================================

CREATE TABLE IF NOT EXISTS staff (
    id              CHAR(36) NOT NULL COMMENT '客服UUID',
    username        VARCHAR(100) NOT NULL COMMENT '登录用户名',
    password_hash   VARCHAR(255) NOT NULL COMMENT 'BCrypt密码',
    name            VARCHAR(100) NOT NULL COMMENT '客服姓名',
    email           VARCHAR(255) NOT NULL COMMENT '邮箱',
    phone           VARCHAR(32) NULL COMMENT '手机号',
    role            VARCHAR(50) NOT NULL DEFAULT 'AGENT'
    COMMENT 'ADMIN或AGENT',
    department      VARCHAR(100) NULL COMMENT '所属部门',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
    COMMENT 'ACTIVE或DISABLED',
    last_login      DATETIME(3) NULL COMMENT '最后登录时间',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_staff_username (username),
    UNIQUE KEY uk_staff_email (email),
    KEY idx_staff_role (role),
    KEY idx_staff_status (status),

    CONSTRAINT chk_staff_role
    CHECK (role IN ('ADMIN', 'AGENT')),
    CONSTRAINT chk_staff_status
    CHECK (status IN ('ACTIVE', 'DISABLED'))
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_0900_ai_ci
    COMMENT='客服人员表';


-- =========================================================
-- 4. 文件处理记录表
-- =========================================================

CREATE TABLE IF NOT EXISTS file_processing_record (
    id                    CHAR(36) NOT NULL COMMENT '记录UUID',
    file_id               CHAR(36) NOT NULL COMMENT '文件业务唯一标识',
    filename              VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_size             BIGINT UNSIGNED NOT NULL COMMENT '文件大小，字节',
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    COMMENT '文件处理状态',
    file_path             VARCHAR(1000) NULL COMMENT '临时文件路径',
    processed_chunks      INT UNSIGNED NULL COMMENT '生成的切片数量',
    embeddings_created    INT UNSIGNED NULL COMMENT '生成的向量数量',
    failure_reason        TEXT NULL COMMENT '处理失败原因',
    upload_time           DATETIME(3) NOT NULL COMMENT '上传时间',
    process_start_time    DATETIME(3) NULL COMMENT '处理开始时间',
    process_end_time      DATETIME(3) NULL COMMENT '处理结束时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_file_processing_file_id (file_id),
    KEY idx_file_processing_status (status),
    KEY idx_file_processing_upload_time (upload_time),
    KEY idx_file_processing_status_time (status, upload_time),

    CONSTRAINT chk_file_processing_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'))
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_0900_ai_ci
    COMMENT='文件处理记录表';


-- =========================================================
-- 5. 知识库文本切片表
-- embedding 不保存到这里，向量保存到 Milvus
-- =========================================================

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id             CHAR(36) NOT NULL COMMENT '切片UUID，同时作为Milvus记录ID',
    file_id        CHAR(36) NOT NULL COMMENT '所属文件业务ID',
    content        LONGTEXT NOT NULL COMMENT '文本切片内容',
    source_file    VARCHAR(255) NOT NULL COMMENT '原始文件名',
    chunk_index    INT UNSIGNED NOT NULL COMMENT '切片序号，从0开始',
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_knowledge_chunk_file_index (file_id, chunk_index),
    KEY idx_knowledge_chunk_file_id (file_id),
    KEY idx_knowledge_chunk_created_at (created_at),

    CONSTRAINT fk_knowledge_chunk_file
    FOREIGN KEY (file_id)
    REFERENCES file_processing_record (file_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_0900_ai_ci
    COMMENT='知识库文本切片表';


-- 中文关键词全文索引
ALTER TABLE knowledge_chunk
    ADD FULLTEXT INDEX ft_knowledge_chunk_content (content)
    WITH PARSER ngram;


-- =========================================================
-- 6. 聊天历史表
-- =========================================================

CREATE TABLE IF NOT EXISTS chat_history (
    id                CHAR(36) NOT NULL COMMENT '聊天记录UUID',
    event_id          VARCHAR(64) NOT NULL COMMENT 'Kafka事件唯一标识',
    session_id        VARCHAR(64) NOT NULL COMMENT '会话标识',
    user_message      TEXT NOT NULL COMMENT '用户问题',
    assistant_reply   LONGTEXT NOT NULL COMMENT '智能客服回复',
    `timestamp`       DATETIME(3) NOT NULL COMMENT '消息发生时间',
    response_time     BIGINT UNSIGNED NULL COMMENT '响应耗时，毫秒',
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_chat_history_event_id (event_id),
    KEY idx_chat_history_session (session_id),
    KEY idx_chat_history_timestamp (`timestamp`),
    KEY idx_chat_history_session_time (session_id, `timestamp`)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_0900_ai_ci
    COMMENT='聊天历史表';