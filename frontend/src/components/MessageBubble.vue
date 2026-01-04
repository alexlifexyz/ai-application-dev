<template>
  <div class="message" :class="[message.role, { streaming: message.isStreaming, error: message.isError }]">
    <div class="avatar" :class="message.role">
      {{ message.role === 'user' ? '👤' : '🤖' }}
    </div>
    
    <div class="content-wrapper">
      <div class="content" ref="contentRef">
        <!-- 始终渲染 Markdown，流式时显示光标 -->
        <div v-html="renderedContent"></div>
        <span v-if="message.isStreaming" class="cursor">▋</span>
      </div>
      
      <div class="meta">
        <span class="time">{{ formatTime(message.timestamp) }}</span>
        <button 
          v-if="!message.isStreaming && message.content"
          class="copy-btn"
          :class="{ success: copied, failed: copyFailed }"
          @click="copyContent"
          :title="copied ? '已复制' : copyFailed ? '复制失败' : '复制内容'"
        >
          <!-- 默认：复制图标 -->
          <svg v-if="!copied && !copyFailed" class="copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
          </svg>
          <!-- 成功：对勾 -->
          <svg v-else-if="copied" class="copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          <!-- 失败：叉 -->
          <svg v-else class="copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
})

const contentRef = ref(null)
const copied = ref(false)

// 配置 marked - 使用自定义 renderer 为代码块添加复制按钮容器
const renderer = new marked.Renderer()

// HTML 实体转义函数
const escapeHtml = (text) => {
  const map = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  }
  return text.replace(/[&<>"']/g, char => map[char])
}

// 自定义代码块渲染，添加复制按钮
renderer.code = function(code, language) {
  const validLang = language && hljs.getLanguage(language) ? language : 'plaintext'
  const langLabel = validLang === 'plaintext' ? '' : validLang
  // 对代码内容进行 HTML 转义，防止 XML/HTML 标签被解析
  const escapedCode = escapeHtml(code)
  return `<div class="code-block-wrapper">
    <div class="code-block-header">
      <span class="code-lang">${langLabel}</span>
      <button class="code-copy-btn" data-code="${encodeURIComponent(code)}" title="复制代码">
        <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none">
          <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
          <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
        </svg>
        <span class="copy-text">复制</span>
      </button>
    </div>
    <pre><code class="language-${validLang}">${escapedCode}</code></pre>
  </div>`
}

marked.setOptions({
  breaks: true,  // 支持换行
  gfm: true,     // GitHub Flavored Markdown
  renderer: renderer
})

// 渲染 Markdown - 始终渲染，不再依赖 isStreaming
const renderedContent = computed(() => {
  if (!props.message.content) return ''
  
  try {
    let content = props.message.content
    
    // 手动将 **text** 转换为 HTML <strong>（支持跨行）
    content = content.replace(/\*\*(.+?)\*\*/gs, '§STRONG§$1§/STRONG§')
    // 手动将 *text* 转换为 HTML <em>（注意避开 **）
    content = content.replace(/(?<!\*)\*([^*]+?)\*(?!\*)/g, '§EM§$1§/EM§')
    
    // 用 marked 解析其他 Markdown
    let html = marked.parse(content)
    
    // 还原 strong 和 em 标签
    html = html.replace(/§STRONG§/g, '<strong>').replace(/§\/STRONG§/g, '</strong>')
    html = html.replace(/§EM§/g, '<em>').replace(/§\/EM§/g, '</em>')
    
    // 使用 DOMPurify 净化 HTML，防止 XSS 攻击
    return DOMPurify.sanitize(html, {
      ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'code', 'pre', 'ul', 'ol', 'li', 'blockquote', 'a', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'span', 'div', 'button', 'svg', 'rect', 'path'],
      ALLOWED_ATTR: ['href', 'target', 'class', 'data-highlighted', 'data-code', 'title', 'viewBox', 'width', 'height', 'stroke', 'stroke-width', 'fill', 'x', 'y', 'rx', 'ry', 'd']
    })
  } catch (e) {
    console.error('Markdown parse error:', e)
    return props.message.content
  }
})

// 监听内容变化，应用代码高亮
watch(() => props.message.content, async () => {
  await nextTick()
  applyHighlight()
  bindCodeCopyEvents()
}, { immediate: true })

// 代码高亮函数
const applyHighlight = () => {
  if (contentRef.value) {
    const codeBlocks = contentRef.value.querySelectorAll('pre code')
    codeBlocks.forEach((block) => {
      if (!block.dataset.highlighted) {
        hljs.highlightElement(block)
        block.dataset.highlighted = 'yes'
      }
    })
  }
}

// 绑定代码复制按钮事件
const bindCodeCopyEvents = () => {
  if (!contentRef.value) return
  
  const copyBtns = contentRef.value.querySelectorAll('.code-copy-btn')
  copyBtns.forEach(btn => {
    // 避免重复绑定
    if (btn.dataset.bindCopy) return
    btn.dataset.bindCopy = 'yes'
    
    btn.addEventListener('click', async (e) => {
      e.preventDefault()
      e.stopPropagation()
      
      const code = decodeURIComponent(btn.dataset.code || '')
      const copyText = btn.querySelector('.copy-text')
      
      try {
        await navigator.clipboard.writeText(code)
        btn.classList.add('copied')
        if (copyText) copyText.textContent = '已复制'
        
        setTimeout(() => {
          btn.classList.remove('copied')
          if (copyText) copyText.textContent = '复制'
        }, 2000)
      } catch (err) {
        console.error('Copy code failed:', err)
        btn.classList.add('failed')
        if (copyText) copyText.textContent = '失败'
        
        setTimeout(() => {
          btn.classList.remove('failed')
          if (copyText) copyText.textContent = '复制'
        }, 2000)
      }
    })
  })
}

// 格式化时间
const formatTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 复制状态
const copyFailed = ref(false)

// 复制内容
const copyContent = async () => {
  try {
    // 检查 Clipboard API 是否可用
    if (!navigator.clipboard) {
      throw new Error('Clipboard API not available')
    }
    await navigator.clipboard.writeText(props.message.content)
    copied.value = true
    copyFailed.value = false
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch (err) {
    console.error('Copy failed:', err)
    copyFailed.value = true
    setTimeout(() => {
      copyFailed.value = false
    }, 2000)
  }
}
</script>

<style lang="scss" scoped>
.message {
  display: flex;
  gap: 12px;
  max-width: 85%;
  
  &.user {
    align-self: flex-end;
    flex-direction: row-reverse;
  }
  
  &.assistant {
    align-self: flex-start;
  }
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  
  &.user {
    background: linear-gradient(135deg, #667eea, #764ba2);
  }
  
  &.assistant {
    background: linear-gradient(135deg, #00c6ff, #0072ff);
  }
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.content {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 15px;
  line-height: 1.6;
  word-wrap: break-word;
  word-break: break-word;
  min-height: 24px;  // 防止空内容时跳动
  
  .message.user & {
    background: var(--bg-message-user);
    color: white;
    border-top-right-radius: 4px;
  }
  
  .message.assistant & {
    background: var(--bg-message-ai);
    color: var(--text-primary);
    border-top-left-radius: 4px;
  }
  
  .message.error & {
    background: rgba(255, 59, 48, 0.1);
    color: #ff3b30;
  }

  // Markdown 样式
  :deep(p) {
    margin: 0 0 8px;
    &:last-child { margin-bottom: 0; }
  }
  
  :deep(ul), :deep(ol) {
    margin: 8px 0;
    padding-left: 20px;
  }
  
  :deep(li) {
    margin: 4px 0;
  }
  
  :deep(code) {
    font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
    font-size: 13px;
  }
  
  :deep(:not(pre) > code) {
    background: rgba(0, 0, 0, 0.1);
    padding: 2px 6px;
    border-radius: 4px;
  }
  
  .message.user & :deep(:not(pre) > code) {
    background: rgba(255, 255, 255, 0.2);
  }
  
  :deep(pre) {
    background: #1e1e1e;
    border-radius: 8px;
    padding: 12px;
    margin: 0;
    overflow-x: auto;
    
    code {
      background: transparent;
      padding: 0;
      color: #d4d4d4;
    }
  }
  
  // 代码块容器
  :deep(.code-block-wrapper) {
    margin: 8px 0;
    border-radius: 8px;
    overflow: hidden;
    background: #1e1e1e;
  }
  
  // 代码块头部
  :deep(.code-block-header) {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: #2d2d2d;
    border-bottom: 1px solid #3d3d3d;
    
    .code-lang {
      font-size: 12px;
      color: #888;
      text-transform: uppercase;
    }
  }
  
  // 代码复制按钮
  :deep(.code-copy-btn) {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 8px;
    border: none;
    background: transparent;
    color: #888;
    font-size: 12px;
    cursor: pointer;
    border-radius: 4px;
    transition: all 0.2s;
    
    &:hover {
      background: rgba(255, 255, 255, 0.1);
      color: #fff;
    }
    
    &.copied {
      color: #4ade80;
    }
    
    &.failed {
      color: #f87171;
    }
    
    svg {
      flex-shrink: 0;
    }
  }
  
  :deep(strong) {
    font-weight: 600;
  }
  
  :deep(a) {
    color: var(--primary-color);
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }
  
  :deep(blockquote) {
    border-left: 3px solid var(--primary-color);
    margin: 8px 0;
    padding-left: 12px;
    color: var(--text-secondary);
  }
}

// 流式文本样式
.streaming-text {
  white-space: pre-wrap;
  word-break: break-word;
}

// 光标闪烁
.cursor {
  display: inline;
  color: var(--primary-color);
  animation: blink 1s infinite;
  font-weight: normal;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

// 元信息
.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  margin-top: 2px;
  
  .message.user & {
    justify-content: flex-end;
  }
}

.time {
  font-size: 11px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
  
  .message.user & {
    color: rgba(255, 255, 255, 0.5);
  }
}

.copy-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s ease;
  
  .copy-icon {
    width: 15px;
    height: 15px;
    color: var(--text-secondary);
    transition: color 0.2s;
  }
  
  // hover 消息时显示
  .message:hover & {
    opacity: 0.6;
  }
  
  &:hover {
    opacity: 1 !important;
    background: var(--bg-hover);
    
    .copy-icon {
      color: var(--primary-color);
    }
  }
  
  // 用户消息特殊样式（浅色图标）
  .message.user & {
    .copy-icon {
      color: rgba(255, 255, 255, 0.6);
    }
    
    &:hover {
      background: rgba(255, 255, 255, 0.15);
      
      .copy-icon {
        color: white;
      }
    }
  }
  
  // 复制成功状态
  &.success {
    opacity: 1 !important;
    .copy-icon { color: #34c759; }
  }
  
  // 复制失败状态
  &.failed {
    opacity: 1 !important;
    .copy-icon { color: #ff3b30; }
  }
}
</style>
