<template>
  <div class="knowledge-panel" :class="{ 'is-open': isOpen }">
    <!-- 面板头部 -->
    <div class="panel-header">
      <h3>📚 知识库</h3>
      <button class="close-btn" @click="$emit('close')" title="关闭">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>
    </div>

    <!-- 统计信息 -->
    <div class="stats-bar">
      <span class="stat-item">
        <strong>{{ stats.totalEntries }}</strong> 条知识
      </span>
      <span class="stat-item">
        <strong>{{ stats.totalSegments }}</strong> 个片段
      </span>
      <span class="stat-item" :title="stats.embeddingModel">
        🧠 本地向量
      </span>
    </div>

    <!-- 添加知识表单 -->
    <div class="add-form">
      <input 
        v-model="newKnowledge.title" 
        type="text" 
        placeholder="知识标题（如：公司简介）"
        class="input-title"
      />
      <textarea 
        v-model="newKnowledge.content" 
        placeholder="粘贴或输入知识内容...&#10;&#10;支持长文本，系统会自动分段处理。&#10;例如：产品说明、FAQ、公司介绍等。"
        class="input-content"
        rows="5"
      ></textarea>
      <button 
        class="btn-add" 
        @click="handleAddKnowledge"
        :disabled="!newKnowledge.title.trim() || !newKnowledge.content.trim() || isAdding"
      >
        {{ isAdding ? '添加中...' : '➕ 添加到知识库' }}
      </button>
    </div>

    <!-- 知识列表 -->
    <div class="knowledge-list">
      <div v-if="knowledgeList.length === 0" class="empty-state">
        <p>📭 知识库为空</p>
        <p class="hint">添加知识后，AI 会优先参考这些内容回答问题</p>
      </div>
      
      <div 
        v-for="item in knowledgeList" 
        :key="item.id" 
        class="knowledge-item"
      >
        <div class="item-header">
          <span class="item-title">{{ item.title }}</span>
          <button class="btn-delete" @click="handleDeleteKnowledge(item.id)" title="删除">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
          </button>
        </div>
        <div class="item-meta">
          <span>📝 {{ formatLength(item.contentLength) }}</span>
          <span>🧩 {{ item.segmentCount }} 片段</span>
          <span>🕐 {{ formatTime(item.createdAt) }}</span>
        </div>
      </div>
    </div>

    <!-- 操作提示 -->
    <div class="panel-footer">
      <p class="tip">💡 提示：知识库内容会自动用于增强 AI 回答</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, defineProps, defineEmits } from 'vue'
import { knowledgeApi } from '../api/knowledge'

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

// 统计信息
const stats = ref({
  totalEntries: 0,
  totalSegments: 0,
  totalCharacters: 0,
  embeddingModel: ''
})

// 知识列表
const knowledgeList = ref([])

// 新知识表单
const newKnowledge = ref({
  title: '',
  content: ''
})

// 添加中状态
const isAdding = ref(false)

// 加载统计和列表
const loadData = async () => {
  try {
    const [statsRes, listRes] = await Promise.all([
      knowledgeApi.getStats(),
      knowledgeApi.listKnowledge()
    ])
    
    if (statsRes.success) {
      stats.value = statsRes.data
    }
    
    if (listRes.success) {
      knowledgeList.value = listRes.data
    }
  } catch (error) {
    console.error('加载知识库数据失败:', error)
  }
}

// 添加知识
const handleAddKnowledge = async () => {
  if (!newKnowledge.value.title.trim() || !newKnowledge.value.content.trim()) return
  
  isAdding.value = true
  try {
    const result = await knowledgeApi.addKnowledge(
      newKnowledge.value.title.trim(),
      newKnowledge.value.content.trim()
    )
    
    if (result.success) {
      // 清空表单
      newKnowledge.value = { title: '', content: '' }
      // 重新加载数据
      await loadData()
    }
  } catch (error) {
    console.error('添加知识失败:', error)
    alert('添加失败，请稍后重试')
  } finally {
    isAdding.value = false
  }
}

// 删除知识
const handleDeleteKnowledge = async (id) => {
  if (!confirm('确定要删除这条知识吗？')) return
  
  try {
    const result = await knowledgeApi.deleteKnowledge(id)
    if (result.success) {
      await loadData()
    }
  } catch (error) {
    console.error('删除知识失败:', error)
    alert('删除失败，请稍后重试')
  }
}

// 格式化文本长度
const formatLength = (length) => {
  if (length < 1000) return `${length} 字`
  return `${(length / 1000).toFixed(1)}k 字`
}

// 格式化时间
const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return date.toLocaleDateString()
}

onMounted(() => {
  loadData()
})

// 暴露刷新方法
defineExpose({ loadData })
</script>

<style lang="scss" scoped>
.knowledge-panel {
  position: fixed;
  right: -400px;
  top: 0;
  width: 380px;
  height: 100vh;
  background: var(--bg-primary);
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  transition: right 0.3s ease;
  z-index: 1000;

  &.is-open {
    right: 0;
  }

  @media (max-width: 768px) {
    width: 100%;
    right: -100%;
  }
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-secondary);

  h3 {
    margin: 0;
    font-size: 18px;
    color: var(--text-primary);
  }

  .close-btn {
    background: none;
    border: none;
    cursor: pointer;
    padding: 4px;
    color: var(--text-secondary);
    border-radius: 4px;
    transition: background 0.2s;

    &:hover {
      background: var(--bg-hover);
    }
  }
}

.stats-bar {
  display: flex;
  gap: 16px;
  padding: 12px 20px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  font-size: 13px;
  color: var(--text-secondary);

  .stat-item {
    display: flex;
    align-items: center;
    gap: 4px;

    strong {
      color: var(--text-primary);
    }
  }
}

.add-form {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 12px;

  .input-title {
    padding: 10px 12px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    font-size: 14px;
    background: var(--bg-primary);
    color: var(--text-primary);
    transition: border-color 0.2s;

    &:focus {
      outline: none;
      border-color: var(--primary-color);
    }
  }

  .input-content {
    padding: 10px 12px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    font-size: 14px;
    background: var(--bg-primary);
    color: var(--text-primary);
    resize: vertical;
    min-height: 100px;
    font-family: inherit;
    line-height: 1.5;
    transition: border-color 0.2s;

    &:focus {
      outline: none;
      border-color: var(--primary-color);
    }
  }

  .btn-add {
    padding: 10px 16px;
    background: var(--primary-color);
    color: white;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    cursor: pointer;
    transition: opacity 0.2s;

    &:hover:not(:disabled) {
      opacity: 0.9;
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

.knowledge-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 20px;

  .empty-state {
    text-align: center;
    padding: 40px 20px;
    color: var(--text-secondary);

    p {
      margin: 8px 0;
    }

    .hint {
      font-size: 13px;
      opacity: 0.8;
    }
  }

  .knowledge-item {
    padding: 12px;
    background: var(--bg-secondary);
    border-radius: 8px;
    margin-bottom: 10px;
    border: 1px solid var(--border-color);

    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .item-title {
        font-weight: 500;
        color: var(--text-primary);
        font-size: 14px;
      }

      .btn-delete {
        background: none;
        border: none;
        cursor: pointer;
        padding: 4px;
        color: var(--text-secondary);
        border-radius: 4px;
        transition: all 0.2s;

        &:hover {
          color: #e74c3c;
          background: rgba(231, 76, 60, 0.1);
        }
      }
    }

    .item-meta {
      display: flex;
      gap: 12px;
      font-size: 12px;
      color: var(--text-tertiary);
    }
  }
}

.panel-footer {
  padding: 12px 20px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-secondary);

  .tip {
    margin: 0;
    font-size: 12px;
    color: var(--text-tertiary);
  }
}
</style>
