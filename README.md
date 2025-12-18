# AI 问答系统（Vue 3 + Spring Boot）

一套前后端分离的 AI 问答/图片生成模板，支持 GPT、Grok、DeepSeek 模型，提供对话记忆、文件上传、图片生成能力，默认使用 MySQL + MyBatis-Plus 持久化对话记录，并通过 SSE 将 AI 回复流式推送到前端。

## 目录结构

- `backend/`：Spring Boot 服务，提供聊天、图片生成、文件上传接口，MyBatis-Plus 管理持久化。
- `frontend/`：Vue 3 + Vite 前端，科技感界面，左侧会话列表、右侧聊天窗口，支持 SSE 实时展示 AI 回复。

## 本地运行

### 前端

```bash
cd frontend
npm install
npm run dev
```

### 后端

```bash
cd backend
./mvnw spring-boot:run  # 若无 mvnw，可使用 mvn spring-boot:run
```

## 必改配置（下载后修改）

1. **数据库**：`backend/src/main/resources/application.yml`
   - `spring.datasource.url`：指向你的 MySQL 实例。
   - `spring.datasource.username` / `spring.datasource.password`：改为你的账号密码。
2. **模型 API Key / Base URL**：同一文件中的 `providers.*`，填写 GPT / Grok / DeepSeek 的密钥和网关地址。
3. **模型是否直连/Mock**：`providers.*.mock`（默认 `true`，本地直接返回模拟结果；设置为 `false` 且填好 `api-key` 后会调用真实接口），模型名称/路径可通过 `model`、`chat-path`、`image-path` 调整。
4. **上传目录（可选）**：`app.upload-dir`，默认 `uploads`。
5. **前端代理（可选）**：`frontend/vite.config.js` 中 `/api` 代理，若后端端口或域名变化请同步调整。

## 主要接口

- `GET /api/chat/conversations`：获取会话列表（按更新时间降序）。
- `GET /api/chat/{id}`：获取指定对话及消息。
- `GET /api/chat/stream`：SSE 流式对话，查询参数包含 `provider`、`prompt`、可选 `conversationId` 与多值 `fileUrls`。
- `POST /api/images`：生成图片。
- `POST /api/files`：上传文件并返回直链。

## 数据库建表 SQL

- 如需手动初始化数据表，执行 `backend/sql/schema.sql`（适配 MySQL 8+），包含 `conversation` 与 `message` 两张表及外键/索引。

## 重要说明

- 后端改为 MyBatis-Plus（不再使用 JPA），自带字段自动填充 `createdAt` / `updatedAt`。
- `ProviderClient` 提供 mock 与直连模式，流式返回通过将回复拆分为小片段并按 SSE 推送，便于无缝替换成供应商的真流式接口。
- 默认返回的图片地址为 mock 字符串，用于演示接口流程；接入真实模型后返回供应商提供的 URL 或 base64 内容即可。
