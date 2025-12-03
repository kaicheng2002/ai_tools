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
3. **上传目录（可选）**：`app.upload-dir`，默认 `uploads`。
4. **前端代理（可选）**：`frontend/vite.config.js` 中 `/api` 代理，若后端端口或域名变化请同步调整。

## 主要接口

- `POST /api/chat`：发起对话，`provider` 选择模型，支持传入 `conversationId` 继续上下文。
- `GET /api/chat/{id}`：获取指定对话及记忆。
- `POST /api/files`：上传文件，返回可复用的文件 URL。
- `POST /api/images`：根据提示词生成图片（示例中为占位调用，按需替换为真实模型接口）。

## 重要说明

- 后端的外部模型调用使用 `ProviderClient` 统一封装，请将 `/mock-endpoint` 替换为真实供应商的聊天/绘图接口路径，并调整请求体结构。
- 当前返回的图片地址为占位字符串，用于演示接口流程；接入真实模型后返回供应商提供的 URL 或 base64 内容即可。
- 项目默认开启 JPA `ddl-auto: update` 以快速启动开发环境，生产环境请根据需要改为受控迁移。
