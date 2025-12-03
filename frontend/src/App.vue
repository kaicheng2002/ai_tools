<template>
  <div class="page">
    <div class="hero">
      <div class="aurora"></div>
      <div class="hero-content">
        <div>
          <p class="eyebrow">未来智能 · 多模型 · 全流程</p>
          <h1>智询空间</h1>
          <p class="lede">连接 GPT / Grok / DeepSeek，一站式对话、记忆、文件与图片生成，专为中国团队打造的沉浸式工作台。</p>
          <div class="chips">
            <span>记忆留存</span>
            <span>附件融入上下文</span>
            <span>智能绘图</span>
          </div>
        </div>
        <div class="hero-card">
          <div class="status">
            <span class="dot"></span>
            <span>实时在线</span>
          </div>
          <div class="stats">
            <div>
              <p class="label">当前模型</p>
              <p class="value">{{ provider }}</p>
            </div>
            <div>
              <p class="label">会话记忆</p>
              <p class="value">{{ conversation ? conversation.messages.length : 0 }} 条</p>
            </div>
          </div>
          <div class="pill">中国式科技美学 · 精准对话体验</div>
        </div>
      </div>
    </div>

    <main class="layout">
      <section class="panel chat-panel">
        <header class="panel-head">
          <div>
            <p class="eyebrow">对话与上下文</p>
            <h2>多模型问答</h2>
          </div>
          <div class="actions-inline">
            <select v-model="provider">
              <option value="GPT">GPT</option>
              <option value="GROK">Grok</option>
              <option value="DEEPSEEK">DeepSeek</option>
            </select>
            <button class="ghost" @click="prompt = ''">清空输入</button>
          </div>
        </header>

        <div class="input-stack">
          <label>你的问题</label>
          <textarea v-model="prompt" rows="3" placeholder="输入问题，模型会结合历史记忆与附件精准回应"></textarea>
          <div class="row">
            <div class="tip">文件会自动加入上下文，保持语义连贯。</div>
            <button class="primary" @click="send" :disabled="loading">{{ loading ? '处理中...' : '发送提问' }}</button>
          </div>
        </div>

        <div class="history" v-if="conversation">
          <div class="history-head">
            <div>
              <p class="eyebrow">对话记忆</p>
              <h3>{{ conversation.title || '未命名会话' }}</h3>
            </div>
            <span class="badge">{{ conversation.messages.length }} 条</span>
          </div>
          <div class="timeline">
            <div v-for="msg in conversation.messages" :key="msg.id" class="bubble" :class="msg.role">
              <div class="meta">
                <span class="role">{{ msg.role === 'user' ? '你' : 'AI' }}</span>
                <span class="model" v-if="msg.provider">{{ msg.provider }}</span>
              </div>
              <p class="content">{{ msg.content }}</p>
              <div v-if="msg.attachments" class="attachments">
                <span>附件：</span>
                <div class="links">
                  <a v-for="link in msg.attachments.split('\n')" :key="link" :href="link" target="_blank" rel="noreferrer">{{ link }}</a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="panel side-panel">
        <div class="block">
          <div class="block-head">
            <div>
              <p class="eyebrow">资料注入</p>
              <h3>上传文件</h3>
            </div>
            <span class="badge soft">同步到上下文</span>
          </div>
          <div class="upload">
            <label class="upload-box">
              <input type="file" @change="uploadFile" />
              <div>
                <p class="title">拖拽或点击上传</p>
                <p class="note">支持快速上传，自动生成直链</p>
              </div>
            </label>
            <p v-if="fileUrl" class="file-link">已上传：{{ fileUrl }}</p>
          </div>
        </div>

        <div class="block">
          <div class="block-head">
            <div>
              <p class="eyebrow">创意出图</p>
              <h3>AI 图片生成</h3>
            </div>
            <span class="badge soft">三模型可切换</span>
          </div>
          <div class="input-stack compact">
            <label>描述你的画面</label>
            <input v-model="imagePrompt" placeholder="如：夜色中带霓虹的未来城市" />
            <div class="row">
              <select v-model="imageProvider">
                <option value="GPT">GPT</option>
                <option value="GROK">Grok</option>
                <option value="DEEPSEEK">DeepSeek</option>
              </select>
              <button class="primary" @click="generateImage" :disabled="imageLoading">{{ imageLoading ? '生成中...' : '生成图片' }}</button>
            </div>
            <div v-if="imageUrl" class="image-preview">
              <p>图片地址（模拟）：</p>
              <code>{{ imageUrl }}</code>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const provider = ref('GPT')
const prompt = ref('')
const conversation = ref(null)
const loading = ref(false)

const fileUrl = ref('')
const imagePrompt = ref('')
const imageProvider = ref('GPT')
const imageLoading = ref(false)
const imageUrl = ref('')

const send = async () => {
  if (!prompt.value) return
  loading.value = true
  try {
    const { data } = await axios.post('/api/chat', {
      provider: provider.value,
      prompt: prompt.value,
      conversationId: conversation.value?.id,
      fileUrls: fileUrl.value ? [fileUrl.value] : []
    })
    conversation.value = data
    prompt.value = ''
  } finally {
    loading.value = false
  }
}

const uploadFile = async (event) => {
  const [file] = event.target.files
  if (!file) return
  const form = new FormData()
  form.append('file', file)
  const { data } = await axios.post('/api/files', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  fileUrl.value = data
}

const generateImage = async () => {
  if (!imagePrompt.value) return
  imageLoading.value = true
  try {
    const { data } = await axios.post('/api/images', {
      provider: imageProvider.value,
      prompt: imagePrompt.value
    })
    imageUrl.value = data
  } finally {
    imageLoading.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 28px;
  color: #e9ecf1;
  font-family: 'Inter', 'HarmonyOS Sans', 'PingFang SC', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  position: relative;
}

.hero {
  background: radial-gradient(circle at 20% 20%, rgba(59, 130, 246, 0.25), transparent 40%),
    radial-gradient(circle at 80% 0%, rgba(111, 66, 193, 0.2), transparent 30%),
    linear-gradient(135deg, #0f172a, #0b1224 50%, #0f172a);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 26px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.35);
}

.aurora {
  position: absolute;
  inset: 0;
  background: radial-gradient(120% 100% at 20% 20%, rgba(56, 189, 248, 0.25), transparent 50%),
    radial-gradient(100% 100% at 90% 10%, rgba(14, 165, 233, 0.2), transparent 45%);
  filter: blur(40px);
  opacity: 0.9;
}

.hero-content {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
}

.hero h1 {
  font-size: 40px;
  margin: 8px 0 10px;
  letter-spacing: 0.5px;
}

.lede {
  margin: 0;
  color: #c7d2fe;
  max-width: 600px;
  line-height: 1.6;
}

.eyebrow {
  margin: 0;
  font-size: 13px;
  letter-spacing: 0.08em;
  color: #7dd3fc;
  text-transform: uppercase;
}

.chips {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.chips span {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: #cbd5f5;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.hero-card {
  min-width: 280px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 18px;
  backdrop-filter: blur(12px);
  box-shadow: 0 10px 40px rgba(59, 130, 246, 0.2);
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.12);
  color: #a7f3d0;
  font-weight: 600;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 10px #34d399;
}

.stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 14px 0;
}

.label {
  color: #a5b4fc;
  font-size: 13px;
  margin: 0 0 6px;
}

.value {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #e0f2fe;
}

.pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2), rgba(37, 99, 235, 0.35));
  color: #dbeafe;
}

.layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 18px;
  margin-top: 20px;
}

.panel {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 18px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.panel h2,
.panel h3 {
  margin: 6px 0 0;
  color: #f8fafc;
}

.actions-inline {
  display: flex;
  align-items: center;
  gap: 10px;
}

.input-stack {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-stack.compact {
  margin-bottom: 0;
}

label {
  color: #cbd5e1;
  font-weight: 600;
}

textarea,
input,
select {
  width: 100%;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  padding: 12px;
  color: #e2e8f0;
  font-size: 14px;
  outline: none;
  transition: border 0.2s ease, box-shadow 0.2s ease;
}

textarea:focus,
input:focus,
select:focus {
  border-color: rgba(96, 165, 250, 0.7);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

textarea {
  resize: vertical;
}

.row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tip {
  color: #a5b4fc;
  font-size: 13px;
}

button {
  border: none;
  cursor: pointer;
  border-radius: 12px;
  padding: 11px 14px;
  font-weight: 700;
  transition: transform 0.15s ease, box-shadow 0.2s ease;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.primary {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  box-shadow: 0 12px 30px rgba(37, 99, 235, 0.4);
}

.ghost {
  background: rgba(255, 255, 255, 0.05);
  color: #cbd5e1;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.history {
  margin-top: 10px;
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.badge {
  background: linear-gradient(135deg, #60a5fa, #7c3aed);
  color: white;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.badge.soft {
  background: rgba(255, 255, 255, 0.08);
  color: #cbd5e1;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bubble {
  position: relative;
  padding: 12px 14px 12px 16px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.25);
}

.bubble::before {
  content: '';
  position: absolute;
  left: -7px;
  top: 14px;
  width: 14px;
  height: 14px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.4), rgba(37, 99, 235, 0.5));
  border-radius: 50%;
  box-shadow: 0 0 0 6px rgba(59, 130, 246, 0.12);
}

.bubble.assistant {
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.25), rgba(59, 130, 246, 0.15));
  border-color: rgba(96, 165, 250, 0.5);
}

.meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.role {
  font-weight: 700;
}

.model {
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: #a5b4fc;
  font-size: 12px;
}

.content {
  margin: 0;
  color: #e5e7eb;
  line-height: 1.5;
}

.attachments {
  margin-top: 8px;
  color: #c7d2fe;
  font-size: 13px;
}

.links {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
}

.links a {
  color: #7dd3fc;
  word-break: break-all;
}

.side-panel .block {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 14px;
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.upload-box {
  border: 1px dashed rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.03);
  cursor: pointer;
  transition: border 0.2s ease, transform 0.2s ease;
}

.upload-box:hover {
  transform: translateY(-1px);
  border-color: rgba(96, 165, 250, 0.7);
}

.upload-box input {
  display: none;
}

.title {
  margin: 0;
  color: #f8fafc;
  font-weight: 700;
}

.note {
  margin: 4px 0 0;
  color: #cbd5e1;
}

.file-link {
  margin-top: 8px;
  color: #a5b4fc;
  word-break: break-all;
}

.image-preview {
  margin-top: 10px;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: #e2e8f0;
}

@media (max-width: 1024px) {
  .hero-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .layout {
    grid-template-columns: 1fr;
  }

  .actions-inline,
  .row {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-card {
    width: 100%;
  }
}
</style>
