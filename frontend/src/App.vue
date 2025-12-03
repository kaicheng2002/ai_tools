<template>
  <div class="page">
    <header>
      <h1>AI 问答工作台</h1>
      <p>GPT / Grok / DeepSeek · 对话记忆 · 文件上传 · 图片生成</p>
    </header>

    <section class="grid">
      <div class="card">
        <h2>对话</h2>
        <div class="form-row">
          <label>选择模型</label>
          <select v-model="provider">
            <option value="GPT">GPT</option>
            <option value="GROK">Grok</option>
            <option value="DEEPSEEK">DeepSeek</option>
          </select>
        </div>
        <div class="form-row">
          <label>问题</label>
          <textarea v-model="prompt" rows="3" placeholder="输入你的问题..." />
        </div>
        <div class="actions">
          <button @click="send" :disabled="loading">{{ loading ? '处理中...' : '发送' }}</button>
        </div>
        <div class="history" v-if="conversation">
          <h3>对话记忆</h3>
          <div v-for="msg in conversation.messages" :key="msg.id" class="bubble" :class="msg.role">
            <strong>{{ msg.role === 'user' ? '你' : 'AI' }}：</strong>
            <p>{{ msg.content }}</p>
            <small v-if="msg.provider">模型：{{ msg.provider }}</small>
          </div>
        </div>
      </div>

      <div class="card">
        <h2>文件上传</h2>
        <input type="file" @change="uploadFile" />
        <p v-if="fileUrl">已上传：{{ fileUrl }}</p>

        <h2>图片生成</h2>
        <div class="form-row">
          <label>提示词</label>
          <input v-model="imagePrompt" placeholder="描述你想要的图片" />
        </div>
        <div class="form-row">
          <label>模型</label>
          <select v-model="imageProvider">
            <option value="GPT">GPT</option>
            <option value="GROK">Grok</option>
            <option value="DEEPSEEK">DeepSeek</option>
          </select>
        </div>
        <div class="actions">
          <button @click="generateImage" :disabled="imageLoading">{{ imageLoading ? '生成中...' : '生成图片' }}</button>
        </div>
        <div v-if="imageUrl" class="image-preview">
          <p>图片地址（模拟）：</p>
          <code>{{ imageUrl }}</code>
        </div>
      </div>
    </section>
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
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  color: #1f2933;
  font-family: 'Inter', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

header {
  text-align: center;
  margin-bottom: 24px;
}

header h1 {
  margin: 0;
  font-size: 32px;
}

header p {
  color: #52606d;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}

.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.card h2 {
  margin-top: 0;
  font-size: 20px;
}

.form-row {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-row label {
  font-weight: 600;
  color: #52606d;
}

input,
select,
textarea,
button {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  box-sizing: border-box;
}

textarea {
  resize: vertical;
}

button {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.1s ease;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

button:hover:not(:disabled) {
  box-shadow: 0 10px 20px rgba(59, 130, 246, 0.2);
  transform: translateY(-1px);
}

.actions {
  margin-top: 8px;
}

.history {
  margin-top: 16px;
}

.bubble {
  padding: 10px 12px;
  border-radius: 10px;
  margin-bottom: 10px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.bubble.assistant {
  background: #eef2ff;
  border-color: #c7d2fe;
}

.bubble small {
  color: #6b7280;
}

.image-preview {
  margin-top: 12px;
  padding: 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  background: #f8fafc;
}
</style>
