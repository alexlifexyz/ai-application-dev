<template>
  <footer class="chat-input">
    <!-- 离线提示 -->
    <div v-if="!isOnline" class="offline-warning">
      ⚠️ 网络已断开，无法发送消息
    </div>
    
    <div class="input-wrapper" :class="{ focused: isFocused, disabled: !isOnline }">
      <textarea
        ref="inputRef"
        v-model="inputText"
        :placeholder="getPlaceholder"
        :disabled="disabled || !isOnline"
        rows="1"
        @focus="isFocused = true"
        @blur="isFocused = false"
        @keydown="handleKeydown"
        @input="autoResize"
      ></textarea>
      
      <!-- 取消按钮（加载中时显示） -->
      <button 
        v-if="isLoading"
        class="cancel-btn" 
        @click="handleCancel"
        title="取消请求"
      >
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
        </svg>
      </button>
      
      <!-- 发送按钮（非加载时显示） -->
      <button 
        v-else
        class="send-btn" 
        :disabled="!canSend"
        @click="handleSend"
      >
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
        </svg>
      </button>
    </div>
    
    <div class="footer-info">
      <span>由 LangChain4j + Spring Boot 驱动</span>
      <span class="divider">•</span>
      <span>支持 Markdown 渲染</span>
    </div>
  </footer>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  disabled: Boolean,
  isLoading: Boolean,
  isOnline: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['send', 'cancel'])

const inputRef = ref(null)
const inputText = ref('')
const isFocused = ref(false)

// 动态 placeholder
const getPlaceholder = computed(() => {
  if (!props.isOnline) return '网络已断开...'
  if (props.disabled) return 'AI 正在思考中...'
  return '输入消息，按 Enter 发送'
})

// 是否可以发送
const canSend = computed(() => {
  return inputText.value.trim().length > 0 && !props.disabled && props.isOnline
})

// 自动调整高度
const autoResize = () => {
  const textarea = inputRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = Math.min(textarea.scrollHeight, 150) + 'px'
  }
}

// 处理键盘事件
const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

// 发送消息
const handleSend = () => {
  if (!canSend.value) return
  
  emit('send', inputText.value)
  inputText.value = ''
  
  // 重置高度
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }
}

// 取消请求
const handleCancel = () => {
  emit('cancel')
}

// 聚焦输入框
onMounted(() => {
  inputRef.value?.focus()
})
</script>

<style lang="scss" scoped>
.chat-input {
  padding: 16px 24px 20px;
  background: var(--bg-chat);
  border-top: 1px solid var(--border-color);
}

.offline-warning {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  margin-bottom: 12px;
  background: rgba(255, 59, 48, 0.1);
  border-radius: 8px;
  font-size: 13px;
  color: #ff3b30;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: 24px;
  padding: 8px 8px 8px 20px;
  transition: all 0.2s;
  
  &.focused {
    border-color: var(--primary-color);
    box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
  }
  
  &.disabled {
    opacity: 0.6;
    background: var(--bg-hover);
  }
}

textarea {
  flex: 1;
  border: none;
  background: transparent;
  resize: none;
  font-size: 15px;
  font-family: inherit;
  line-height: 1.5;
  padding: 8px 0;
  max-height: 150px;
  color: var(--text-primary);
  
  &::placeholder {
    color: var(--text-muted);
  }
  
  &:focus {
    outline: none;
  }
  
  &:disabled {
    cursor: not-allowed;
  }
}

.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: var(--primary-color);
  color: white;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  
  &:hover:not(:disabled) {
    background: var(--primary-hover);
    transform: scale(1.05);
  }
  
  &:disabled {
    background: var(--text-muted);
    cursor: not-allowed;
  }
}

.cancel-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: #ff3b30;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  
  &:hover {
    background: #ff453a;
    transform: scale(1.05);
  }
}

.footer-info {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 11px;
  color: var(--text-muted);
  
  .divider {
    opacity: 0.5;
  }
}
</style>
