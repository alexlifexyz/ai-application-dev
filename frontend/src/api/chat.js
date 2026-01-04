/**
 * Chat API 封装
 */

const API_BASE = '/api/chat'

export const chatApi = {
  /**
   * 流式对话
   * @param {string} sessionId - 会话 ID
   * @param {string} message - 用户消息
   * @param {Function} onToken - 收到 token 的回调
   * @param {Function} onComplete - 完成的回调
   * @param {Function} onError - 错误的回调
   * @param {Object} options - 可选配置 { timeout: 60000 }
   * @returns {AbortController} 返回 AbortController 供外部取消请求
   */
  async streamChat(sessionId, message, onToken, onComplete, onError, options = {}) {
    const controller = new AbortController()
    const timeout = options.timeout || 60000 // 默认 60 秒超时
    
    // 设置超时自动取消
    const timeoutId = setTimeout(() => {
      controller.abort()
      onError(new Error('请求超时，请稍后重试'))
    }, timeout)
    
    try {
      const response = await fetch(`${API_BASE}/conversation/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream'
        },
        body: JSON.stringify({
          sessionId,
          message
        }),
        signal: controller.signal
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      let currentEvent = ''  // 跟踪当前事件类型

      while (true) {
        const { done, value } = await reader.read()
        
        if (done) {
          clearTimeout(timeoutId)
          onComplete()
          break
        }

        // 解码数据
        buffer += decoder.decode(value, { stream: true })
        
        // SSE 事件由双换行分隔
        const events = buffer.split(/\n\n/)
        // 保留最后一个可能不完整的事件
        buffer = events.pop() || ''

        for (const event of events) {
          if (!event.trim()) continue
          
          const lines = event.split('\n')
          const dataLines = []  // 收集所有 data 行
          
          for (const line of lines) {
            // 解析事件类型
            if (line.startsWith('event:')) {
              currentEvent = line.slice(6).trim()
            }
            // 解析数据 - 收集所有 data: 行（SSE 规范：多行 data 用换行拼接）
            else if (line.startsWith('data:')) {
              dataLines.push(line.slice(5))
            }
          }
          
          // 按 SSE 规范，多行 data 用换行符连接
          const eventData = dataLines.join('\n')
          
          // console.log('[SSE Debug] Event:', currentEvent, 'Data:', JSON.stringify(eventData))
          
          // 检查是否是完成事件
          if (currentEvent === 'done' || eventData.trim() === '[DONE]') {
            clearTimeout(timeoutId)
            onComplete()
            return
          }
          
          // 发送 token（直接使用原始数据，不 trim）
          if (eventData !== '') {
            // console.log('[SSE Debug] Sending token:', JSON.stringify(eventData))
            onToken(eventData)
          }
          
          // 重置事件类型
          currentEvent = ''
        }
      }
    } catch (error) {
      clearTimeout(timeoutId)
      // 区分用户主动取消和其他错误
      if (error.name === 'AbortError') {
        console.log('Request was cancelled')
        // 不调用 onError，因为可能是用户主动取消
      } else {
        console.error('Stream chat error:', error)
        onError(error)
      }
    }
    
    return controller
  },
  
  /**
   * 取消正在进行的请求
   * @param {AbortController} controller - streamChat 返回的 controller
   */
  cancelRequest(controller) {
    if (controller) {
      controller.abort()
    }
  },

  /**
   * 普通对话（非流式）
   * @param {string} sessionId - 会话 ID
   * @param {string} message - 用户消息
   * @returns {Promise<Object>}
   */
  async chat(sessionId, message) {
    const response = await fetch(`${API_BASE}/conversation`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        sessionId,
        message
      })
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    return response.json()
  },

  /**
   * 清除会话
   * @param {string} sessionId - 会话 ID
   * @returns {Promise<Object>}
   */
  async clearSession(sessionId) {
    const response = await fetch(`${API_BASE}/conversation/${sessionId}`, {
      method: 'DELETE'
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    return response.json()
  }
}
