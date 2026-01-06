import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { v4 as uuidv4 } from 'uuid'
import { chatApi } from '../api/chat'

/**
 * 聊天状态管理 Store
 * 
 * 管理聊天会话、消息列表、加载状态等
 */
export const useChatStore = defineStore('chat', () => {
  // ==================== 工具函数 ====================
  
  const safeGetStorage = (key, defaultValue = '') => {
    try {
      return localStorage.getItem(key) || defaultValue
    } catch (e) {
      console.warn('localStorage 读取失败:', e)
      return defaultValue
    }
  }

  const safeSetStorage = (key, value) => {
    try {
      localStorage.setItem(key, value)
    } catch (e) {
      console.warn('localStorage 写入失败:', e)
    }
  }

  // ==================== State ====================
  
  // 会话 ID
  const sessionId = ref(safeGetStorage('chatSessionId') || uuidv4())
  safeSetStorage('chatSessionId', sessionId.value)

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

  // 网络状态
  const isOnline = ref(navigator.onLine)

  // 当前请求的 AbortController
  let currentRequestController = null

  // ==================== Getters ====================
  
  const messageCount = computed(() => messages.value.length)
  
  const lastMessage = computed(() => 
    messages.value.length > 0 ? messages.value[messages.value.length - 1] : null
  )

  const hasMessages = computed(() => messages.value.length > 1)

  // ==================== Actions ====================

  /**
   * 发送消息
   */
  const sendMessage = async (content) => {
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
    let contentBuffer = ''

    const getMessageIndex = () => messages.value.findIndex(m => m.id === aiMessageId)

    try {
      currentRequestController = await chatApi.streamChat(
        sessionId.value,
        content.trim(),
        // onToken
        (token) => {
          contentBuffer += token
          const index = getMessageIndex()
          if (index !== -1) {
            messages.value[index] = {
              ...messages.value[index],
              content: contentBuffer
            }
          }
        },
        // onComplete
        () => {
          const index = getMessageIndex()
          if (index !== -1) {
            messages.value[index] = {
              ...messages.value[index],
              isStreaming: false,
              timestamp: new Date()
            }
          }
          isLoading.value = false
          currentRequestController = null
        },
        // onError
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

  /**
   * 清除聊天记录
   */
  const clearChat = async () => {
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

  /**
   * 新建会话
   */
  const newChat = () => {
    cancelRequest()
    sessionId.value = uuidv4()
    safeSetStorage('chatSessionId', sessionId.value)
    messages.value = [{
      id: 'welcome',
      role: 'assistant',
      content: '你好！我是你的 AI 助手。有什么我可以帮你的吗？',
      timestamp: new Date()
    }]
  }

  /**
   * 取消当前请求
   */
  const cancelRequest = () => {
    if (currentRequestController) {
      chatApi.cancelRequest(currentRequestController)
      currentRequestController = null
      isLoading.value = false
    }
  }

  /**
   * 设置网络状态
   */
  const setOnlineStatus = (status) => {
    isOnline.value = status
  }

  return {
    // State
    sessionId,
    messages,
    isLoading,
    isOnline,
    
    // Getters
    messageCount,
    lastMessage,
    hasMessages,
    
    // Actions
    sendMessage,
    clearChat,
    newChat,
    cancelRequest,
    setOnlineStatus
  }
})
