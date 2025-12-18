<template>
  <div class="app-shell">
    <aside class="sidebar">
      <header class="brand">
        <div class="logo">智</div>
        <div>
          <p class="brand-name">智答工作台</p>
          <p class="brand-sub">中国式科技感 · 流畅问答</p>
        </div>
      </header>
      <div class="side-actions">
        <button class="primary" @click="startNewConversation">＋ 新建对话</button>
        <div class="provider-select">
          <label>选择模型</label>
          <select v-model="provider">
            <option value="GPT">GPT</option>
            <option value="GROK">Grok</option>
            <option value="DEEPSEEK">DeepSeek</option>
          </select>
        </div>
      </div>

      <section class="side-block">
        <div class="block-head">
          <span>历史记录</span>
          <span class="badge">{{ conversations.length }}</span>
        </div>
        <div class="conversation-list">
          <div
            v-for="item in conversations"
            :key="item.id"
            :class="['conversation-card', item.id === conversationId ? 'active' : '']"
            @click="selectConversation(item.id)"
          >
            <p class="title">{{ item.title }}</p>
            <p class="meta">更新时间：{{ formatTime(item.updatedAt) }}</p>
          </div>
          <p v-if="!conversations.length" class="empty">暂无对话，先开启一轮吧。</p>
        </div>
      </section>

      <section class="side-block soft">
        <div class="block-head">
          <span>文件注入</span>
          <span class="badge ghost">同步上下文</span>
        </div>
        <label class="upload-box">
          <input type="file" @change="uploadFile" />
          <div>
            <p class="title">上传资料</p>
            <p class="note">直链可自动加入提问上下文</p>
          </div>
        </label>
        <p v-if="fileUrl" class="file-link">已上传：{{ fileUrl }}</p>
      </section>

      <section class="side-block soft">
        <div class="block-head">
          <span>图片生成</span>
          <span class="badge ghost">创意实验室</span>
        </div>
        <div class="image-box">
          <input v-model="imagePrompt" placeholder="描述想要的画面" />
          <div class="image-actions">
            <select v-model="imageProvider">
              <option value="GPT">GPT</option>
              <option value="GROK">Grok</option>
              <option value="DEEPSEEK">DeepSeek</option>
            </select>
            <button class="ghost" :disabled="imageLoading" @click="generateImage">
              {{ imageLoading ? '生成中...' : '生成' }}
            </button>
          </div>
          <p v-if="imageUrl" class="file-link">图片地址：{{ imageUrl }}</p>
        </div>
      </section>
    </aside>

    <section class="chat-area">
      <header class="chat-head">
        <div>
          <p class="eyebrow">实时流式 · 记忆加持</p>
          <h1>{{ activeTitle }}</h1>
          <p class="hint">左侧是所有对话，右侧记录完整问答。AI 回复将通过 SSE 连续输出。</p>
        </div>
        <div class="status-chip">
          <span class="dot"></span> 在线
        </div>
      </header>

      <div class="chat-window" ref="chatView">
        <div v-if="messages.length" class="message-stack">
          <div v-for="msg in messages" :key="msg.id" :class="['msg', msg.role]">
            <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
            <div class="bubble">
              <div class="bubble-head">
                <span class="role">{{ msg.role === 'user' ? '我' : '智能助手' }}</span>
                <span class="provider" v-if="msg.provider">{{ msg.provider }}</span>
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
        <div v-else class="empty-chat">
          <p class="title">开始一次有温度的对话</p>
          <p class="note">输入问题后，右侧自动展示你和 AI 的气泡聊天，内容实时滚动。</p>
        </div>
      </div>

      <div class="composer">
        <div class="composer-top">
          <textarea
            v-model="prompt"
            placeholder="请输入问题，AI 将结合历史与附件实时作答"
            rows="3"
            :disabled="sending"
          ></textarea>
        </div>
        <div class="composer-actions">
          <div class="tags">
            <span v-if="fileUrl" class="tag">已绑定附件</span>
            <span class="tag ghost">模型：{{ provider }}</span>
          </div>
          <div class="buttons">
            <button class="ghost" @click="prompt = ''" :disabled="sending">清空</button>
            <button class="primary" @click="send" :disabled="!prompt || sending">{{ sending ? '生成中...' : '发送' }}</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import axios from 'axios'

const conversations = ref([])
const conversationId = ref(null)
const messages = ref([])
const provider = ref('GPT')
const prompt = ref('')
const sending = ref(false)
const chatView = ref(null)

const fileUrl = ref('')
const imagePrompt = ref('')
const imageProvider = ref('GPT')
const imageLoading = ref(false)
const imageUrl = ref('')

let eventSource = null
let streamingMessage = null

const activeTitle = computed(() => {
  if (!messages.value.length) return '全新对话'
  return conversations.value.find((c) => c.id === conversationId.value)?.title || '会话详情'
})

const formatTime = (value) => {
  if (!value) return '刚刚'
  return new Date(value).toLocaleString()
}

const loadConversations = async () => {
  const { data } = await axios.get('/api/chat/conversations')
  conversations.value = data
}

const selectConversation = async (id) => {
  conversationId.value = id
  const { data } = await axios.get(`/api/chat/${id}`)
  messages.value = data.messages || []
  await nextTick()
  scrollChat()
}

const startNewConversation = () => {
  conversationId.value = null
  messages.value = []
  prompt.value = ''
}

const send = async () => {
  if (!prompt.value || sending.value) return
  sending.value = true

  const userMessage = {
    id: `local-${Date.now()}`,
    role: 'user',
    content: prompt.value,
    attachments: fileUrl.value,
    provider: null
  }
  messages.value.push(userMessage)
  const payloadPrompt = prompt.value
  prompt.value = ''

  openStream(payloadPrompt)
}

const openStream = (payloadPrompt) => {
  const params = new URLSearchParams()
  params.append('provider', provider.value)
  params.append('prompt', payloadPrompt)
  if (conversationId.value) {
    params.append('conversationId', conversationId.value)
  }
  if (fileUrl.value) {
    params.append('fileUrls', fileUrl.value)
  }

  if (eventSource) {
    eventSource.close()
  }

  eventSource = new EventSource(`/api/chat/stream?${params.toString()}`)

  eventSource.addEventListener('meta', (event) => {
    const meta = JSON.parse(event.data)
    if (!conversationId.value) {
      conversationId.value = meta.conversationId
    }
  })

  eventSource.addEventListener('chunk', async (event) => {
    if (!streamingMessage) {
      streamingMessage = { id: `ai-${Date.now()}`, role: 'assistant', provider: provider.value, content: '' }
      messages.value.push(streamingMessage)
    }
    streamingMessage.content += event.data
    await nextTick()
    scrollChat()
  })

  eventSource.addEventListener('done', async () => {
    await refreshConversation()
    closeStream()
  })

  eventSource.onerror = () => {
    closeStream()
  }
}

const refreshConversation = async () => {
  await loadConversations()
  if (conversationId.value) {
    await selectConversation(conversationId.value)
  }
  sending.value = false
  streamingMessage = null
}

const closeStream = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  sending.value = false
  streamingMessage = null
}

const uploadFile = async (event) => {
  const [file] = event.target.files
  if (!file) return
  const form = new FormData()
  form.append('file', file)
  const { data } = await axios.post('/api/files', form, { headers: { 'Content-Type': 'multipart/form-data' } })
  fileUrl.value = data
}

const generateImage = async () => {
  if (!imagePrompt.value) return
  imageLoading.value = true
  try {
    const { data } = await axios.post('/api/images', {
      prompt: imagePrompt.value,
      provider: imageProvider.value
    })
    imageUrl.value = data
  } finally {
    imageLoading.value = false
  }
}

const scrollChat = () => {
  if (!chatView.value) return
  chatView.value.scrollTo({ top: chatView.value.scrollHeight, behavior: 'smooth' })
}

onMounted(async () => {
  await loadConversations()
  if (conversations.value.length) {
    await selectConversation(conversations.value[0].id)
  }
})
</script>

<style scoped>
:global(body) {
  margin: 0;
  background: radial-gradient(circle at 20% 20%, rgba(60, 148, 255, 0.18), transparent 30%),
    radial-gradient(circle at 80% 10%, rgba(255, 94, 180, 0.14), transparent 25%),
    #0b1021;
  font-family: 'SF Pro Display', 'Inter', system-ui, -apple-system, sans-serif;
  color: #e6ecf5;
}

.app-shell {
  display: grid;
  grid-template-columns: 340px 1fr;
  height: 100vh;
}

.sidebar {
  background: rgba(255, 255, 255, 0.02);
  backdrop-filter: blur(10px);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #5cd0ff, #5d89ff);
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #0b1021;
  font-size: 20px;
}

.brand-name {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.brand-sub {
  margin: 2px 0 0;
  color: #9fb2c8;
  font-size: 13px;
}

.side-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.primary {
  background: linear-gradient(135deg, #5cd0ff, #5d89ff);
  color: #0b1021;
  border: none;
  padding: 10px 14px;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.15s ease;
}

.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.primary:hover:enabled {
  transform: translateY(-1px);
}

.provider-select {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 10px;
}

.provider-select select,
.image-actions select,
select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #e6ecf5;
  border-radius: 8px;
  padding: 8px;
}

.side-block {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 16px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-block.soft {
  background: rgba(255, 255, 255, 0.03);
}

.block-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #c8d9ec;
  font-size: 14px;
}

.badge {
  background: rgba(255, 255, 255, 0.08);
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  color: #e6ecf5;
}

.badge.ghost {
  background: rgba(92, 208, 255, 0.12);
  color: #9bdfff;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 260px;
  overflow: auto;
}

.conversation-card {
  padding: 10px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.02);
  cursor: pointer;
  transition: border 0.2s ease, background 0.2s ease;
}

.conversation-card.active {
  border-color: #5cd0ff;
  background: rgba(92, 208, 255, 0.08);
}

.conversation-card .title {
  margin: 0;
  font-weight: 600;
}

.conversation-card .meta {
  margin: 4px 0 0;
  color: #9fb2c8;
  font-size: 12px;
}

.empty {
  color: #7c8aa5;
  text-align: center;
}

.upload-box {
  border: 1px dashed rgba(255, 255, 255, 0.18);
  border-radius: 12px;
  padding: 14px;
  cursor: pointer;
  display: block;
  background: rgba(255, 255, 255, 0.03);
}

.upload-box input {
  display: none;
}

.upload-box .title {
  margin: 0;
  font-weight: 600;
}

.upload-box .note {
  margin: 4px 0 0;
  color: #9fb2c8;
  font-size: 13px;
}

.file-link {
  font-size: 12px;
  color: #9bdfff;
  word-break: break-all;
}

.image-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.image-box input {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #e6ecf5;
  border-radius: 8px;
  padding: 8px;
}

.image-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.ghost {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: #e6ecf5;
  padding: 8px 12px;
  border-radius: 10px;
  cursor: pointer;
}

.chat-area {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 24px;
  gap: 16px;
}

.chat-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.eyebrow {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #7c8aa5;
  font-size: 12px;
}

.chat-head h1 {
  margin: 6px 0 4px;
}

.hint {
  margin: 0;
  color: #9fb2c8;
  font-size: 14px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(92, 208, 255, 0.12);
  color: #9bdfff;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #5cd0ff;
  box-shadow: 0 0 8px #5cd0ff;
}

.chat-window {
  flex: 1;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 18px;
  padding: 16px;
  overflow: auto;
}

.message-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.msg {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.msg.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: rgba(92, 208, 255, 0.18);
  display: grid;
  place-items: center;
  font-weight: 700;
}

.msg.user .avatar {
  background: rgba(255, 255, 255, 0.12);
}

.bubble {
  max-width: 70%;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 12px 14px;
}

.msg.user .bubble {
  background: rgba(92, 208, 255, 0.1);
  border-color: rgba(92, 208, 255, 0.4);
}

.bubble-head {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: #9fb2c8;
}

.provider {
  background: rgba(92, 208, 255, 0.12);
  color: #9bdfff;
  padding: 2px 6px;
  border-radius: 999px;
}

.content {
  margin: 6px 0 0;
  white-space: pre-wrap;
}

.attachments {
  margin-top: 8px;
  font-size: 12px;
  color: #9fb2c8;
}

.attachments .links {
  display: flex;
  flex-direction: column;
}

.attachments a {
  color: #9bdfff;
}

.empty-chat {
  text-align: center;
  color: #9fb2c8;
}

.composer {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.composer textarea {
  width: 100%;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #e6ecf5;
  border-radius: 12px;
  padding: 12px;
  resize: vertical;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tags {
  display: flex;
  gap: 8px;
  align-items: center;
}

.tag {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(92, 208, 255, 0.12);
  color: #9bdfff;
  font-size: 12px;
}

.tag.ghost {
  background: rgba(255, 255, 255, 0.06);
  color: #c8d9ec;
}

.buttons {
  display: flex;
  gap: 8px;
}
</style>
