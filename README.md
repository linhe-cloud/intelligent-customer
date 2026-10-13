# Intelligent Customer

基于 Spring Boot 和 RAG 技术实现的智能客服系统。

系统支持文档上传、异步解析、文本切片、向量化、知识库检索和智能客服问答。系统只根据知识库内容回答问题，检索不到相关知识时会转人工提示，避免模型自由编造答案。

## 功能

- JWT 登录认证和角色权限控制
- 客户、客服、管理员账号管理
- TXT、PDF、DOCX、Excel 文档解析
- 文本清洗、文本切片和 BGE 向量化
- MySQL 全文检索和 Milvus 语义检索
- RRF 混合检索
- 基于知识库的 RAG 智能客服
- Redis 会话上下文
- Kafka 异步文档处理和聊天记录保存
- Kafka 重试和死信队列

## 系统流程

```text
文档上传 -> 文件校验 -> 文件保存 -> Kafka
        -> 文档解析 -> 文本清洗 -> 文本切片
        -> BGE 向量化 -> MySQL 保存文本 -> Milvus 保存向量

用户提问 -> BGE 生成问题向量
        -> Milvus 语义检索 + MySQL 关键词检索
        -> RRF 混合排序 -> 大语言模型生成客服回复
```

## 技术栈

| 技术 | 用途 |
|---|---|
| Java 17 | 开发语言 |
| Spring Boot 3.4.3 | Web 框架 |
| Spring AI | 大语言模型调用 |
| Spring Security + JWT | 认证和权限控制 |
| MyBatis-Plus | 数据访问 |
| MySQL | 业务数据和知识文本 |
| Redis | 会话历史 |
| Kafka | 异步消息处理 |
| Milvus | 向量数据库 |
| BGE-M3 | 文本向量模型 |
| DeepSeek | 客服回复模型 |

## 项目结构

```text
IntelligentCustomer
├── intelligent-customer-common       公共异常和组件
├── intelligent-customer-framework    JWT、Kafka、Milvus、BGE 和安全配置
├── intelligent-customer-system      领域模型、服务、仓储和消费者
├── intelligent-customer-admin       Controller、配置和启动类
├── docker/milvus                    Milvus Docker 配置
├── sql/init.sql                     数据库初始化脚本
├── .env.example                     环境变量示例
└── pom.xml                          Maven 父工程
```

## 环境要求

- JDK 17
- MySQL 8+
- Redis
- Kafka
- Docker
- Milvus
- DeepSeek API Key
- SiliconFlow BGE API Key

## 启动基础服务

### Milvus

```bash
docker compose -f docker/milvus/compose.yml up -d
docker ps
```

Milvus 默认地址：`http://localhost:19530`

### Redis

```bash
brew services start redis
```

Redis 默认地址：`localhost:6379`。

### Kafka

Kafka 默认地址：`localhost:9092`。检查端口：

```bash
nc -vz localhost 9092
```

## 数据库初始化

```sql
CREATE DATABASE intelligent_customer
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

```bash
mysql -u root -p intelligent_customer < sql/init.sql
```

## 环境变量

复制配置文件：

```bash
cp .env.example .env
```

`.env` 至少需要配置以下内容：

```properties
DB_USERNAME=root
DB_PASSWORD=你的MySQL密码

JWT_SECRET=你的随机JWT密钥
JWT_EXPIRATION=86400000

DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_API_KEY=你的DeepSeek密钥
DEEPSEEK_MODEL=deepseek-chat

BGE_BASE_URL=https://api.siliconflow.cn/v1
BGE_API_KEY=你的BGE密钥
BGE_MODEL=BAAI/bge-m3
BGE_DIMENSION=1024

MILVUS_URI=http://localhost:19530
MILVUS_TOKEN=
MILVUS_DATABASE=default
MILVUS_COLLECTION=knowledge_vectors
MILVUS_DIMENSION=1024
MILVUS_METRIC_TYPE=COSINE
```

生成 JWT 密钥：

```bash
openssl rand -hex 32
```

不要将 `.env` 提交到 GitHub。

## 启动项目

```bash
./mvnw spring-boot:run -pl intelligent-customer-admin
```

项目地址：`http://localhost:8081`

编译测试：

```bash
./mvnw clean test
```

打包：

```bash
./mvnw clean package -DskipTests
```

## API 示例

以下接口默认运行在 `http://localhost:8081`。

### 登录

```bash
curl -X POST "http://localhost:8081/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teststaff",
    "password": "Test123456",
    "userType": "STAFF"
  }'
```

从返回值的 `data.token` 中获取 Token：

```bash
TOKEN="你的JWT_TOKEN"
```

### 上传文档

```bash
curl -X POST "http://localhost:8081/document/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/document.pdf"
```

支持：`txt`、`pdf`、`docx`、`xlsx`、`xls`。

文档状态：

```text
PENDING -> PROCESSING -> SUCCESS
                         |
                         -> FAILED
```

查询状态：

```bash
curl "http://localhost:8081/document/status/文件ID" \
  -H "Authorization: Bearer $TOKEN"
```

失败后重试：

```bash
curl -X POST "http://localhost:8081/document/文件ID/retry" \
  -H "Authorization: Bearer $TOKEN"
```

### 混合检索

```bash
curl -X POST "http://localhost:8081/search/hybrid" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "质量问题如何申请退货",
    "topk": 5
  }'
```

`topk` 的有效范围是 1 到 20。

### 语义检索

```bash
curl -X POST "http://localhost:8081/search/semantic" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "物流三天没有更新应该怎么办？",
    "topk": 5
  }'
```

### 智能客服

```bash
curl -X POST "http://localhost:8081/chat" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "我的商品有质量问题，应该如何申请退货？"
  }'
```

继续当前会话：

```bash
curl -X POST "http://localhost:8081/chat" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "上一次返回的sessionId",
    "message": "那退货运费由谁承担？"
  }'
```

没有相关知识时，系统返回：

```text
抱歉，我无法回答这个问题，建议联系人工客服进一步处理。
```

## 权限说明

| 接口 | 权限 |
|---|---|
| `/auth/login` | 公开 |
| `/auth/register` | 公开，仅允许注册 CUSTOMER |
| `/chat` | 登录用户 |
| `/search/**` | STAFF |
| `/document/**` | STAFF |
| `/embedding/**` | STAFF |
| `GET /customer/**` | STAFF |
| 修改和删除客户 | ADMIN |
| `/staff/**` | ADMIN |

## Kafka 消息

```text
文档上传：document-upload
聊天记录：chat-message
文档死信：document-upload-dlq
聊天死信：chat-message-dlq
```

消息失败后会自动重试，超过重试次数后进入对应死信队列。

## 测试结果

已验证：

- Maven 编译和测试
- JWT 登录和权限认证
- 文件上传、状态查询和重试
- TXT、PDF、DOCX、Excel 文件解析
- BGE 向量化和 Milvus 检索
- MySQL 关键词检索和 RRF 混合检索
- RAG 智能客服和 Redis 会话上下文
- Kafka 文档和聊天记录处理
- 搜索接口参数校验

执行测试：

```bash
./mvnw clean test
```

## 安全注意事项

以下内容不会提交到 GitHub：

```text
.env
test-data/
data/uploads/
docker/milvus/volumes/
```

不要在代码或 README 中写入真实的数据库密码、JWT 密钥、DeepSeek API Key 和 BGE API Key。

## 后续计划

- 增加完整的单元测试和集成测试
- 增加文档处理失败补偿机制
- 增加意图识别和客服话术库
- 增加 Tool Calling 和 MCP 能力
- 增加前端管理界面、监控和日志查询
