import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { knowledgeApi } from '../api/knowledge'

/**
 * 知识库状态管理 Store
 * 
 * 管理知识库数据、统计信息、加载状态等
 */
export const useKnowledgeStore = defineStore('knowledge', () => {
  // ==================== State ====================
  
  // 知识列表
  const knowledgeList = ref([])

  // 统计信息
  const stats = ref({
    totalEntries: 0,
    totalSegments: 0,
    embeddingModel: ''
  })

  // 加载状态
  const isLoading = ref(false)

  // 添加中状态
  const isAdding = ref(false)

  // 面板是否打开
  const isPanelOpen = ref(false)

  // 详情弹窗
  const detailModal = ref({
    visible: false,
    loading: false,
    data: null
  })

  // ==================== Getters ====================
  
  const totalEntries = computed(() => stats.value.totalEntries)
  const totalSegments = computed(() => stats.value.totalSegments)
  const hasKnowledge = computed(() => knowledgeList.value.length > 0)

  // ==================== Actions ====================

  /**
   * 加载知识列表
   */
  const loadKnowledgeList = async () => {
    isLoading.value = true
    try {
      const response = await knowledgeApi.listKnowledge()
      if (response.success) {
        knowledgeList.value = response.data || []
      }
    } catch (error) {
      console.error('加载知识列表失败:', error)
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 加载统计信息
   */
  const loadStats = async () => {
    try {
      const response = await knowledgeApi.getStats()
      if (response.success) {
        stats.value = response.data
      }
    } catch (error) {
      console.error('加载统计信息失败:', error)
    }
  }

  /**
   * 加载所有数据
   */
  const loadData = async () => {
    await Promise.all([loadKnowledgeList(), loadStats()])
  }

  /**
   * 添加知识
   */
  const addKnowledge = async (title, content) => {
    if (isAdding.value) return false
    
    isAdding.value = true
    try {
      const response = await knowledgeApi.addKnowledge(title, content)
      if (response.success) {
        await loadData()
        return true
      }
      return false
    } catch (error) {
      console.error('添加知识失败:', error)
      return false
    } finally {
      isAdding.value = false
    }
  }

  /**
   * 删除知识
   */
  const deleteKnowledge = async (id) => {
    try {
      const response = await knowledgeApi.deleteKnowledge(id)
      if (response.success) {
        await loadData()
        return true
      }
      return false
    } catch (error) {
      console.error('删除知识失败:', error)
      return false
    }
  }

  /**
   * 查看知识详情
   */
  const viewDetail = async (id) => {
    detailModal.value.visible = true
    detailModal.value.loading = true
    detailModal.value.data = null
    
    try {
      const response = await knowledgeApi.getKnowledgeDetail(id)
      if (response.success) {
        detailModal.value.data = response.data
      }
    } catch (error) {
      console.error('加载知识详情失败:', error)
    } finally {
      detailModal.value.loading = false
    }
  }

  /**
   * 关闭详情弹窗
   */
  const closeDetailModal = () => {
    detailModal.value.visible = false
    detailModal.value.data = null
  }

  /**
   * 检索知识
   */
  const searchKnowledge = async (query, maxResults = 5) => {
    try {
      const response = await knowledgeApi.searchKnowledge(query, maxResults)
      if (response.success) {
        return response.data || []
      }
      return []
    } catch (error) {
      console.error('检索知识失败:', error)
      return []
    }
  }

  /**
   * 切换面板
   */
  const togglePanel = () => {
    isPanelOpen.value = !isPanelOpen.value
    if (isPanelOpen.value) {
      loadData()
    }
  }

  /**
   * 关闭面板
   */
  const closePanel = () => {
    isPanelOpen.value = false
  }

  return {
    // State
    knowledgeList,
    stats,
    isLoading,
    isAdding,
    isPanelOpen,
    detailModal,
    
    // Getters
    totalEntries,
    totalSegments,
    hasKnowledge,
    
    // Actions
    loadKnowledgeList,
    loadStats,
    loadData,
    addKnowledge,
    deleteKnowledge,
    viewDetail,
    closeDetailModal,
    searchKnowledge,
    togglePanel,
    closePanel
  }
})
