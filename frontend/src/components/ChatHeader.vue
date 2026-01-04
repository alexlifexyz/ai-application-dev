<template>
  <header class="chat-header">
    <div class="header-left">
      <div class="logo">
        <span class="logo-icon">🤖</span>
        <h1>AI 智能助手</h1>
      </div>
      <div class="status" :class="{ offline: !isOnline }">
        <span class="status-dot"></span>
        <span class="status-text">{{ isOnline ? '在线' : '离线' }}</span>
      </div>
    </div>
    
    <div class="header-right">
      <button class="icon-btn" @click="$emit('new-chat')" title="新建会话">
        <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none">
          <path d="M12 5v14M5 12h14"/>
        </svg>
      </button>
      <button class="icon-btn danger" @click="handleClear" title="清除记忆">
        <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none">
          <polyline points="3 6 5 6 21 6"/>
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
        </svg>
      </button>
    </div>
  </header>
</template>

<script setup>
const props = defineProps({
  sessionId: String,
  isOnline: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['clear', 'new-chat'])

const handleClear = () => {
  if (confirm('确定要清除当前对话的记忆吗？')) {
    emit('clear')
  }
}
</script>

<style lang="scss" scoped>
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: var(--bg-chat);
  border-bottom: 1px solid var(--border-color);
  backdrop-filter: blur(10px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .logo-icon {
    font-size: 24px;
  }
  
  h1 {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }
}

.status {
  display: flex;
  align-items: center;
  gap: 6px;
  
  .status-dot {
    width: 8px;
    height: 8px;
    background: #34c759;
    border-radius: 50%;
    animation: pulse 2s infinite;
  }
  
  .status-text {
    font-size: 12px;
    color: var(--text-secondary);
  }
  
  // 离线状态样式
  &.offline {
    .status-dot {
      background: #ff3b30;
      animation: none;
    }
    
    .status-text {
      color: #ff3b30;
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.header-right {
  display: flex;
  gap: 8px;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 8px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }
  
  &.danger:hover {
    background: rgba(255, 59, 48, 0.1);
    color: #ff3b30;
  }
}
</style>
