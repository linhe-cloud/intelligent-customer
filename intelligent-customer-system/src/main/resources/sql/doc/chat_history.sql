CREATE TABLE chat_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),      -- 唯一标识
    event_id VARCHAR(225) UNIQUE NOT NULL,              -- 事件标识
    session_id VARCHAR(225) NOT NULL,                   -- 会话标识
    user_message TEXT NOT NULL,                         -- 用户消息
    assistant_reply TEXT NOT NULL,                      -- 模型回复
    timestamp TIMESTAMP NOT NULL,                       -- 时间戳
    response_time BIGINT,                               -- 响应时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP      -- 创建时间
);
CREATE INDEX idx_chat_history_session ON chat_history(session_id);
