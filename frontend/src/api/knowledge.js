/**
 * 知识库 API 封装
 * 
 * 提供知识库的增删改查操作
 */

const API_BASE = '/api/knowledge'

export const knowledgeApi = {
  /**
   * 添加知识到知识库
   * @param {string} title - 知识标题
   * @param {string} content - 知识内容
   * @returns {Promise<Object>} 返回 { success, id, message }
   */
  async addKnowledge(title, content) {
    const response = await fetch(API_BASE, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ title, content })
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    return response.json()
  },

  /**
   * 获取知识列表
   * @returns {Promise<Object>} 返回 { success, data: KnowledgeEntry[], total }
   */
  async listKnowledge() {
    const response = await fetch(API_BASE)
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    return response.json()
  },

  /**
   * 删除知识条目
   * @param {string} id - 知识条目 ID
   * @returns {Promise<Object>} 返回 { success, message }
   */
  async deleteKnowledge(id) {
    const response = await fetch(`${API_BASE}/${id}`, {
      method: 'DELETE'
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    return response.json()
  },

  /**
   * 检索相关知识
   * @param {string} query - 查询文本
   * @param {number} maxResults - 最大返回数量（默认 5）
   * @returns {Promise<Object>} 返回 { success, data: RelevantKnowledge[], total }
   */
  async searchKnowledge(query, maxResults = 5) {
    const response = await fetch(`${API_BASE}/search`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ query, maxResults })
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    return response.json()
  },

  /**
   * 获取知识库统计信息
   * @returns {Promise<Object>} 返回 { success, data: KnowledgeStats }
   */
  async getStats() {
    const response = await fetch(`${API_BASE}/stats`)
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    return response.json()
  }
}
