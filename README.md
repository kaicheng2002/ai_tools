# AI 问答系统（Vue 3 + Spring Boot）

一套前后端分离的 AI 问答/图片生成模板，支持 GPT、Grok、DeepSeek 模型，提供对话记忆、文件上传、图片生成能力，默认使用 MySQL 持久化对话记录。

## 目录结构

- `backend/`：Spring Boot 服务，提供聊天、图片生成、文件上传接口，持久化对话记忆。
- `frontend/`：Vue 3 + Vite 前端，内置简洁操作界面。

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

- `POST /api/chat`：发起对话，`provider` 选择模型，支持传入 `conversationId` 继续上下文；`fileUrls` 可把上传结果写入提示（后端记忆区会保存附件链接）。
- `GET /api/chat/{id}`：获取指定对话及记忆。
- `POST /api/files`：上传文件，返回可复用的文件 URL（形如 `/api/files/{filename}`）。
- `POST /api/images`：根据提示词生成图片（默认返回 mock 地址，关闭 mock 后按真实供应商返回结果）。

## 重要说明

- `ProviderClient` 提供 mock（本地字符串）与直连模式，可按供应商接口要求修改 `chatPayload` / `imagePayload` 构造。
- 默认返回的图片地址为 mock 字符串，用于演示接口流程；接入真实模型后返回供应商提供的 URL 或 base64 内容即可。
- 项目默认开启 JPA `ddl-auto: update` 以快速启动开发环境，生产环境请根据需要改为受控迁移。
