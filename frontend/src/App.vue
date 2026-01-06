<template>
  <div class="app-container">
    <ChatHeader 
      :session-id="chatStore.sessionId"
      :is-online="chatStore.isOnline"
      @clear="chatStore.clearChat" 
      @new-chat="chatStore.newChat"
      @cancel-request="chatStore.cancelRequest"
      @toggle-knowledge="knowledgeStore.togglePanel"
    />
    <ChatMessages 
      :messages="chatStore.messages" 
      :is-loading="chatStore.isLoading"
      ref="messagesRef"
    />
    <ChatInput 
      :disabled="chatStore.isLoading || !chatStore.isOnline"
      :is-loading="chatStore.isLoading"
      :is-online="chatStore.isOnline"
      @send="handleSend"
      @cancel="chatStore.cancelRequest"
    />
    
    <!-- 遮罩层 + 知识库面板 -->
    <Transition name="overlay-fade">
      <div 
        v-if="knowledgeStore.isPanelOpen" 
        class="overlay" 
        @click="knowledgeStore.closePanel"
      ></div>
    </Transition>
    
    <Transition name="panel-slide">
      <KnowledgePanel 
        v-if="knowledgeStore.isPanelOpen"
        :is-open="knowledgeStore.isPanelOpen" 
        @close="knowledgeStore.closePanel"
      />
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import ChatHeader from './components/ChatHeader.vue'
import ChatMessages from './components/ChatMessages.vue'
import ChatInput from './components/ChatInput.vue'
import KnowledgePanel from './components/KnowledgePanel.vue'
import { useChatStore, useKnowledgeStore } from './stores'

// ==================== Pinia Stores ====================
const chatStore = useChatStore()
const knowledgeStore = useKnowledgeStore()

// ==================== 组件引用 ====================
const messagesRef = ref(null)

// ==================== 网络状态监听 ====================
const handleOnline = () => chatStore.setOnlineStatus(true)
const handleOffline = () => chatStore.setOnlineStatus(false)

// ==================== 消息发送 ====================
const scrollToBottom = async () => {
  await nextTick()
  messagesRef.value?.scrollToBottom()
}

const handleSend = async (content) => {
  await scrollToBottom()
  await chatStore.sendMessage(content)
}

// ==================== 生命周期 ====================
onMounted(() => {
  scrollToBottom()
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)
})

onUnmounted(() => {
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)
  chatStore.cancelRequest()
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

.overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  z-index: 999;
  cursor: pointer;
}

// 遮罩层淡入淡出动画
.overlay-fade-enter-active,
.overlay-fade-leave-active {
  transition: opacity 0.3s ease;
}

.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
}

// 面板滑入滑出动画
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.panel-slide-enter-from,
.panel-slide-leave-to {
  transform: translateX(100%);
}
</style>
