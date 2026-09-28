CREATE TABLE file_processing_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),      -- 唯一标识
    file_id VARCHAR(255) UNIQUE NOT NULL,               -- 文件标识
    filename VARCHAR(255) NOT NULL,                     -- 文件名
    file_size BIGINT,                                   -- 文件大小
    status VARCHAR(50) NOT NULL,                        -- 状态
    file_path VARCHAR(500),                             -- 文件路径
    processed_chunks INT,                               -- 处理的分块数
    embeddings_created INT,                             -- 创建的向量数
    failure_reason TEXT,                                -- 失败原因
    upload_time TIMESTAMP NOT NULL,                     -- 上传时间
    process_start_time TIMESTAMP,                       -- 处理开始时间
    process_end_time TIMESTAMP                          -- 处理结束时间
);

CREATE INDEX idx_file_processing_file_id ON file_processing_record(file_id);
CREATE INDEX idx_file_processing_status ON file_processing_record(status);
