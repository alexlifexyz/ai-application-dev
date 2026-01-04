<template>
  <main class="chat-messages" ref="containerRef">
    <div class="messages-content">
      <MessageBubble
        v-for="message in messages"
        :key="message.id"
        :message="message"
      />
      
      <!-- 加载指示器 -->
      <div v-if="isLoading && !lastMessageIsStreaming" class="loading-indicator">
        <div class="typing-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>
    
    <!-- 锚点元素：浏览器会自动保持此元素在视口内 -->
    <div ref="anchorRef" class="scroll-anchor"></div>
  </main>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import MessageBubble from './MessageBubble.vue'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  isLoading: Boolean
})

const containerRef = ref(null)
const anchorRef = ref(null)

// 检查最后一条消息是否正在流式传输
const lastMessageIsStreaming = computed(() => {
  const lastMsg = props.messages[props.messages.length - 1]
  return lastMsg?.isStreaming
})

// 滚动到底部 - 使用 scrollIntoView 更可靠
const scrollToBottom = () => {
  nextTick(() => {
    anchorRef.value?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  })
}

// 监听消息数量变化，新消息时滚动
watch(() => props.messages.length, () => {
  scrollToBottom()
})

defineExpose({
  scrollToBottom
})
</script>

<style lang="scss" scoped>
.chat-messages {
  flex: 1;
  overflow-y: auto;
  overflow-anchor: none; // 禁用默认锚点，我们用自定义锚点
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.messages-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
}

// 滚动锚点 - 浏览器会自动保持此元素可见
.scroll-anchor {
  overflow-anchor: auto;
  height: 1px;
  flex-shrink: 0;
}

// 加载指示器
.loading-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg-message-ai);
  border-radius: 16px;
  width: fit-content;
  margin-left: 52px; // 对齐消息内容
}

.typing-dots {
  display: flex;
  gap: 4px;
  
  span {
    width: 8px;
    height: 8px;
    background: var(--text-secondary);
    border-radius: 50%;
    animation: bounce 1.4s infinite ease-in-out both;
    
    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
