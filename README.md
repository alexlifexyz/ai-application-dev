<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen?logo=spring-boot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/LangChain4j-1.10.0-blue?logo=chainlink" alt="LangChain4j">
  <img src="https://img.shields.io/badge/Chroma-v2%20API-orange?logo=databricks" alt="Chroma">
  <img src="https://img.shields.io/badge/Vue.js-3.4-42b883?logo=vue.js" alt="Vue.js">
  <img src="https://img.shields.io/badge/Java-17+-orange?logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

<h1 align="center">🤖 AI Chat Application</h1>

<p align="center">
  <strong>一个开箱即用的 AI 对话应用脚手架</strong><br>
  基于 Spring Boot + LangChain4j + Vue 3 构建，支持流式对话、多轮会话记忆、Chroma 向量数据库持久化
</p>

<p align="center">
  <a href="#-特性">特性</a> •
  <a href="#-快速开始">快速开始</a> •
  <a href="#-项目结构">项目结构</a> •
  <a href="#-api-文档">API 文档</a> •
  <a href="#-自定义配置">配置</a> •
  <a href="#-技术栈">技术栈</a>
</p>

---

## ✨ 特性

### 🎯 核心功能
- **流式对话** — SSE 实时响应，打字机效果输出
- **多轮会话** — 上下文记忆，支持连续对话
- **RAG 知识库** — 本地向量检索，零 API 成本，支持自定义知识增强
- **Markdown 渲染** — 完整支持代码高亮、表格、列表等
- **代码复制** — 一键复制代码块和消息内容

### 🛠 开发者友好
- **开箱即用** — 克隆即可运行，零配置启动演示模式
- **多 LLM 支持** — 兼容 OpenAI、阿里通义、DeepSeek 等 OpenAI 兼容接口
- **本地向量模型** — all-MiniLM-L6-v2，384 维向量，无需额外 API Key
- **API 文档** — 内置 Swagger UI，接口一目了然
- **前后端分离** — 清晰的架构，易于扩展

### 🧠 RAG 知识增强
- **Chroma 持久化** — 支持 Chroma v2 API，知识数据持久存储，重启不丢失
- **远程 Embedding** — 支持通义千问 text-embedding-v3（1024维），无需本地模型
- **智能检索** — 余弦相似度匹配，精准找到相关知识
- **自动分段** — 长文本智能切分（500字/段，50字重叠）
- **详情查看** — 点击知识条目可查看完整内容和分段详情 🆕
- **实时生效** — 添加知识后立即可用，无需重启
- **可视化管理** — 侧边栏面板，便捷的增删改查操作

### 🎨 精致 UI
- **响应式设计** — 适配桌面和移动端
- **深色模式** — 自动跟随系统主题
- **流畅动画** — 消息气泡、滚动等细节动效

---

## 🚀 快速开始

### 环境要求
- Java 17+
- Node.js 18+（仅开发前端时需要）
- Maven 3.6+

### 1. 克隆项目
```bash
git clone https://github.com/alexlifexyz/ai-application-dev.git
cd ai-application-dev
```

### 2. 配置 API Key
```bash
# 方式一：环境变量（推荐）
export OPENAI_API_KEY=your-api-key
export OPENAI_BASE_URL=https://api.openai.com/v1  # 可选，默认 OpenAI

# 方式二：修改配置文件 src/main/resources/application.yml
```

<details>
<summary>📋 支持的 LLM 服务商配置示例</summary>

| 服务商 | BASE_URL | 模型示例 |
|--------|----------|----------|
| OpenAI | `https://api.openai.com/v1` | `gpt-4`, `gpt-3.5-turbo` |
| 阿里通义 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-turbo`, `qwen-plus` |
| DeepSeek | `https://api.deepseek.com/v1` | `deepseek-chat` |
| 硅基流动 | `https://api.siliconflow.cn/v1` | `Qwen/Qwen2-7B-Instruct` |
| 月之暗面 | `https://api.moonshot.cn/v1` | `moonshot-v1-8k` |

</details>

### 3. 启动应用
```bash
# 编译并启动
mvn spring-boot:run

# 或者打包后运行
mvn clean package -DskipTests
java -jar target/ai-application-dev-1.0.0.jar
```

### 4. 访问应用
- 🌐 **Web 界面**: http://localhost:8080
- 📖 **API 文档**: http://localhost:8080/swagger-ui.html

---

## 📁 项目结构

```
ai-application-dev/
├── 📂 src/main/java/com/alex/ai/
│   ├── AiApplicationDevApplication.java    # 启动类
│   ├── 📂 config/
│   │   ├── CorsConfig.java                 # CORS 配置
│   │   ├── LangChain4jConfig.java          # LangChain4j 配置
│   │   └── OpenApiConfig.java              # Swagger 配置
│   ├── 📂 controller/
│   │   ├── ChatController.java             # 对话 API 控制器
│   │   └── KnowledgeController.java        # 知识库 API 控制器 🆕
│   ├── 📂 model/
│   │   ├── ChatRequest.java                # 请求 DTO
│   │   └── ChatResponse.java               # 响应 DTO
│   └── 📂 service/
│       ├── ChatService.java                # 基础聊天服务
│       ├── ConversationService.java        # 多轮对话服务（含流式 + RAG）
│       ├── EmbeddingService.java           # 向量嵌入服务 🆕
│       └── KnowledgeService.java           # 知识库管理服务 🆕
│
├── 📂 src/main/resources/
│   ├── application.yml                     # 应用配置
│   └── 📂 static/                          # 前端构建产物
│
├── 📂 frontend/                            # Vue 3 前端源码
│   ├── 📂 src/
│   │   ├── App.vue                         # 根组件
│   │   ├── 📂 api/
│   │   │   ├── chat.js                     # 对话 API（含 SSE 处理）
│   │   │   └── knowledge.js                # 知识库 API 🆕
│   │   ├── 📂 components/
│   │   │   ├── ChatHeader.vue              # 头部导航
│   │   │   ├── ChatInput.vue               # 输入组件
│   │   │   ├── ChatMessages.vue            # 消息列表
│   │   │   ├── MessageBubble.vue           # 消息气泡（Markdown 渲染）
│   │   │   └── KnowledgePanel.vue          # 知识库管理面板 🆕
│   │   └── 📂 styles/
│   │       └── main.scss                   # 全局样式
│   └── package.json
│
└── pom.xml                                 # Maven 配置
```

---

## 📖 API 文档

### 接口概览

#### 对话接口

| 方法 | 路径 | 描述 |
|------|------|------|
| `POST` | `/api/chat/simple` | 简单对话（单轮） |
| `POST` | `/api/chat/with-context` | 带系统提示词的对话 |
| `POST` | `/api/chat/conversation` | 多轮对话（保持上下文） |
| `POST` | `/api/chat/conversation/stream` | 流式多轮对话（SSE，自动 RAG 增强） |
| `DELETE` | `/api/chat/conversation/{sessionId}` | 清除会话记忆 |
| `GET` | `/api/chat/health` | 健康检查 |

#### 知识库接口 🆕

| 方法 | 路径 | 描述 |
|------|------|------|
| `POST` | `/api/knowledge` | 添加知识到知识库 |
| `GET` | `/api/knowledge` | 获取知识列表 |
| `GET` | `/api/knowledge/{id}` | 获取知识详情（含分段内容）🆕 |
| `DELETE` | `/api/knowledge/{id}` | 删除指定知识 |
| `POST` | `/api/knowledge/search` | 检索相关知识（向量相似度） |
| `GET` | `/api/knowledge/stats` | 获取知识库统计信息 |

### 请求示例

#### 流式对话（推荐）
```bash
curl -X POST http://localhost:8080/api/chat/conversation/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "sessionId": "user-123",
    "message": "用 Java 写一个快速排序"
  }'
```

#### 简单对话
```bash
curl -X POST http://localhost:8080/api/chat/simple \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，介绍一下你自己"}'
```

#### 添加知识到知识库 🆕
```bash
curl -X POST http://localhost:8080/api/knowledge \
  -H "Content-Type: application/json" \
  -d '{
    "title": "产品介绍",
    "content": "我们的产品支持 RAG 知识库检索，使用本地向量模型..."
  }'
```

#### 检索知识 🆕
```bash
curl -X POST http://localhost:8080/api/knowledge/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "产品有哪些功能",
    "maxResults": 3
  }'
```

---

## ⚙️ 自定义配置

### 核心配置项

```yaml
# src/main/resources/application.yml

langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY:your-key}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
      model-name: ${OPENAI_MODEL:gpt-3.5-turbo}
      temperature: 0.7          # 创造性（0-2）
      max-tokens: 2000          # 最大输出长度
      timeout: 60s              # 请求超时
```

### 前端开发

```bash
cd frontend

# 安装依赖
npm install

# 开发模式（热重载）
npm run dev

# 构建生产版本（输出到 src/main/resources/static）
npm run build
```

---

## 🔧 技术栈

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.1 | Web 框架 |
| LangChain4j | 1.10.0 | LLM 编排框架（升级版） |
| Chroma | v2 API | 向量数据库（持久化存储）🆕 |
| text-embedding-v3 | 1024维 | 通义千问远程嵌入模型 🆕 |
| OpenAI API | - | 大模型接口 |
| Lombok | - | 简化代码 |
| Springdoc | - | API 文档生成 |

### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue.js | 3.4 | 响应式框架 |
| Vite | 5.0 | 构建工具 |
| marked | 11.0 | Markdown 解析 |
| highlight.js | 11.9 | 代码高亮 |
| DOMPurify | 3.3 | XSS 防护 |

---

## � RAG 知识库功能详解

### 什么是 RAG？

**RAG (Retrieval-Augmented Generation)** = 检索增强生成

传统对话：`用户问题 → LLM → 回答（可能不准确或幻觉）`

RAG 对话：`用户问题 → 检索知识库 → [问题+相关知识] → LLM → 准确回答`

### 核心优势

- ✅ **准确性提升** — 基于真实知识回答，减少 AI 幻觉
- ✅ **私有数据** — 企业内部文档、产品手册等不会被 LLM 训练
- ✅ **实时更新** — 添加新知识立即生效，无需重新训练模型
- ✅ **成本节省** — 使用本地向量模型，无额外 API 费用
- ✅ **可溯源** — 回答附带来源，增强可信度

### 使用场景

| 场景 | 示例 |
|------|------|
| **企业知识库** | 产品说明书、技术文档、FAQ |
| **客服系统** | 售后问题、常见故障排查 |
| **内部助手** | 公司规章制度、流程指南 |
| **教育培训** | 课程资料、考试大纲 |
| **法律咨询** | 法律条文、判例分析 |

### 快速体验

1. **启动应用**后，点击右上角 📚 图标打开知识库面板
2. **添加知识**：
   ```
   标题：公司产品介绍
   内容：我们的主打产品是 AI 智能助手，支持流式对话、多轮会话、RAG 检索...
   ```
3. **测试对话**：
   ```
   问：你们公司的产品有哪些功能？
   答：[引用知识库] 根据参考资料，我们的产品支持...
   ```

### 技术实现

```java
// 1. 文本向量化（使用 all-MiniLM-L6-v2 模型）
Embedding vector = embeddingModel.embed("产品介绍文本").content();
// → [0.123, -0.456, ..., 0.789]  (384 维向量)

// 2. 存储到向量数据库
embeddingStore.add(vector, textSegment);

// 3. 相似度检索（余弦相似度）
List<Match> results = embeddingStore.findRelevant(queryVector, 3);
// → 返回最相关的 3 条知识

// 4. 构建增强 Prompt
String prompt = "【参考资料】\n" + results + "\n【用户问题】\n" + query;

// 5. LLM 基于增强 Prompt 生成回答
String answer = chatModel.generate(prompt);
```

### 配置说明

```java
// src/main/java/com/alex/ai/service/KnowledgeService.java

private static final int SEGMENT_SIZE = 500;    // 分段大小（字符）
private static final int SEGMENT_OVERLAP = 50;  // 重叠大小（保持上下文）
private static final int MAX_RESULTS = 3;       // 检索数量
private static final double MIN_SCORE = 0.5;    // 最低相似度阈值
```

### 进阶优化

#### 向量存储方案选择

本项目支持两种向量存储方案，通过配置切换：

| 方案 | 适用场景 | 特点 |
|------|----------|------|
| **memory**（默认） | 开发测试、Demo 演示 | 零依赖，重启后数据丢失 |
| **chroma** | 生产环境、持久化需求 | 需要 Docker，数据持久化 |

#### 方案一：内存存储（默认）

```yaml
# application.yml（默认配置，无需修改）
rag:
  vector-store:
    type: memory
```

**优势**：开箱即用，无需额外服务  
**劣势**：重启后知识库数据丢失

#### 方案二：Chroma 持久化存储

**Step 1：启动 Chroma 服务**
```bash
# 方式一：使用 Python（推荐）
pip install chromadb
chroma run --host localhost --port 8000 --path ./chroma

# 方式二：使用 Docker
docker run -d --name chroma \
  -p 8000:8000 \
  -v chroma-data:/chroma/chroma \
  chromadb/chroma:latest

# 验证服务（Chroma v2 API）
curl http://localhost:8000/api/v2/heartbeat
```

**Step 2：修改配置**
```yaml
# application.yml
rag:
  vector-store:
    type: chroma
    chroma:
      base-url: http://localhost:8000
      collection-name: ai-knowledge
  embedding-model: text-embedding-v3  # 通义千问嵌入模型
```

**或通过环境变量**
```bash
export RAG_VECTOR_STORE_TYPE=chroma
export CHROMA_BASE_URL=http://localhost:8000
export CHROMA_COLLECTION=ai-knowledge
export RAG_EMBEDDING_MODEL=text-embedding-v3
mvn spring-boot:run
```

**优势**：数据持久化，支持百万级向量，重启后知识库自动恢复  
**劣势**：需要部署 Chroma 服务

#### 方案对比

| 特性 | InMemory | Chroma |
|------|----------|--------|
| **部署复杂度** | ⭐（零依赖） | ⭐⭐（需 Python/Docker） |
| **数据持久化** | ❌ | ✅ |
| **重启恢复** | ❌ | ✅（自动恢复知识条目）|
| **性能** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **最大数据量** | ~100 万（受内存限制） | ~1000 万 |
| **生产推荐** | ❌ | ✅ |

#### 支持文件上传（规划中）

```java
// TODO: 支持 PDF、Word、Markdown 等文件解析
// 依赖：langchain4j-document-parser-apache-pdfbox
```

---

## �🤝 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

---

## 📄 License

本项目采用 [MIT License](LICENSE) 开源协议。

---

<p align="center">
  <sub>如果这个项目对你有帮助，请给个 ⭐️ Star 支持一下！</sub>
</p>
