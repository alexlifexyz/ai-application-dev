<template>
  <div class="app-container">
    <ChatHeader 
      :session-id="sessionId"
      :is-online="isOnline"
      @clear="handleClearChat" 
      @new-chat="handleNewChat"
      @cancel-request="handleCancelRequest"
    />
    <ChatMessages 
      :messages="messages" 
      :is-loading="isLoading"
      ref="messagesRef"
    />
    <ChatInput 
      :disabled="isLoading || !isOnline"
      :is-loading="isLoading"
      :is-online="isOnline"
      @send="handleSend"
      @cancel="handleCancelRequest"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { v4 as uuidv4 } from 'uuid'
import ChatHeader from './components/ChatHeader.vue'
import ChatMessages from './components/ChatMessages.vue'
import ChatInput from './components/ChatInput.vue'
import { chatApi } from './api/chat'

// ========== 工具函数 ==========

/**
 * 安全地从 localStorage 获取值
 * @param {string} key - 键名
 * @param {string} defaultValue - 默认值
 * @returns {string}
 */
const safeGetStorage = (key, defaultValue = '') => {
  try {
    return localStorage.getItem(key) || defaultValue
  } catch (e) {
    console.warn('localStorage 读取失败:', e)
    return defaultValue
  }
}

/**
 * 安全地向 localStorage 写入值
 * @param {string} key - 键名
 * @param {string} value - 值
 */
const safeSetStorage = (key, value) => {
  try {
    localStorage.setItem(key, value)
  } catch (e) {
    console.warn('localStorage 写入失败（可能是隐私模式或存储已满）:', e)
  }
}

// ========== 网络状态检测 ==========
const isOnline = ref(navigator.onLine)

const handleOnline = () => {
  isOnline.value = true
}

const handleOffline = () => {
  isOnline.value = false
}

// ========== 会话管理 ==========
// 会话 ID（使用安全的 localStorage 操作）
const sessionId = ref(safeGetStorage('chatSessionId') || uuidv4())
safeSetStorage('chatSessionId', sessionId.value)

// 当前请求的 AbortController（用于取消请求）
let currentRequestController = null

// 消息列表
const messages = ref([
  {
    id: 'welcome',
    role: 'assistant',
    content: '你好！我是你的 AI 助手。有什么我可以帮你的吗？',
    timestamp: new Date()
  }
])

// 加载状态
const isLoading = ref(false)

// 消息列表组件引用
const messagesRef = ref(null)

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  messagesRef.value?.scrollToBottom()
}

// 发送消息
const handleSend = async (content) => {
  if (!content.trim() || isLoading.value) return

  // 添加用户消息
  const userMessage = {
    id: uuidv4(),
    role: 'user',
    content: content.trim(),
    timestamp: new Date()
  }
  messages.value.push(userMessage)

  // 创建 AI 消息占位
  const aiMessageId = uuidv4()
  messages.value.push({
    id: aiMessageId,
    role: 'assistant',
    content: '',
    timestamp: new Date(),
    isStreaming: true
  })

  isLoading.value = true
  await scrollToBottom()

  // 用于收集内容的变量
  let contentBuffer = ''

  // 获取消息索引（用于响应式更新）
  const getMessageIndex = () => messages.value.findIndex(m => m.id === aiMessageId)

  try {
    // 使用流式 API，保存 controller 以便取消
    currentRequestController = await chatApi.streamChat(
      sessionId.value,
      content.trim(),
      // onToken 回调 - 不再手动滚动，由 CSS overflow-anchor 自动处理
      (token) => {
        contentBuffer += token
        const index = getMessageIndex()
        if (index !== -1) {
          // 使用索引方式更新，确保 Vue 响应式
          messages.value[index] = {
            ...messages.value[index],
            content: contentBuffer
          }
        }
      },
      // onComplete 回调
      () => {
        const index = getMessageIndex()
        if (index !== -1) {
          // 更新消息状态
          messages.value[index] = {
            ...messages.value[index],
            isStreaming: false,
            timestamp: new Date()
          }
        }
        isLoading.value = false
        currentRequestController = null
      },
      // onError 回调
      (error) => {
        console.error('Stream error:', error)
        const index = getMessageIndex()
        if (index !== -1) {
          messages.value[index] = {
            ...messages.value[index],
            content: contentBuffer || (error.message || '抱歉，请求失败了。请稍后重试。'),
            isStreaming: false,
            isError: true
          }
        }
        isLoading.value = false
        currentRequestController = null
      }
    )
  } catch (error) {
    console.error('Error:', error)
    const index = getMessageIndex()
    if (index !== -1) {
      messages.value[index] = {
        ...messages.value[index],
        content: '网络连接失败，请检查您的网络设置。',
        isStreaming: false,
        isError: true
      }
    }
    isLoading.value = false
  }
}

// 清除聊天记录
const handleClearChat = async () => {
  try {
    await chatApi.clearSession(sessionId.value)
    messages.value = [{
      id: 'welcome',
      role: 'assistant',
      content: '会话已清除，让我们重新开始吧！',
      timestamp: new Date()
    }]
  } catch (error) {
    console.error('Clear session error:', error)
  }
}

// 新建会话
const handleNewChat = () => {
  // 如果有正在进行的请求，先取消
  handleCancelRequest()
  
  sessionId.value = uuidv4()
  safeSetStorage('chatSessionId', sessionId.value)
  messages.value = [{
    id: 'welcome',
    role: 'assistant',
    content: '你好！我是你的 AI 助手。有什么我可以帮你的吗？',
    timestamp: new Date()
  }]
}

// 取消当前请求
const handleCancelRequest = () => {
  if (currentRequestController) {
    chatApi.cancelRequest(currentRequestController)
    currentRequestController = null
    isLoading.value = false
  }
}

onMounted(() => {
  scrollToBottom()
  // 监听网络状态变化
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)
})

onUnmounted(() => {
  // 清理网络状态监听器
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)
  // 取消可能正在进行的请求
  handleCancelRequest()
})
</script>

<style lang="scss" scoped>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 900px;
  margin: 0 auto;
  background: var(--bg-chat);
  box-shadow: 0 0 40px rgba(0, 0, 0, 0.1);

  @media (min-width: 768px) {
    height: 95vh;
    margin-top: 2.5vh;
    border-radius: 16px;
    overflow: hidden;
  }
}
</style>
