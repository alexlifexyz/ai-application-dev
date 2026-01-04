<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen?logo=spring-boot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/LangChain4j-0.35.0-blue?logo=chainlink" alt="LangChain4j">
  <img src="https://img.shields.io/badge/Vue.js-3.4-42b883?logo=vue.js" alt="Vue.js">
  <img src="https://img.shields.io/badge/Java-17+-orange?logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

<h1 align="center">🤖 AI Chat Application</h1>

<p align="center">
  <strong>一个开箱即用的 AI 对话应用脚手架</strong><br>
  基于 Spring Boot + LangChain4j + Vue 3 构建，支持流式对话、多轮会话记忆
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
- **Markdown 渲染** — 完整支持代码高亮、表格、列表等
- **代码复制** — 一键复制代码块内容

### 🛠 开发者友好
- **开箱即用** — 克隆即可运行，零配置启动演示模式
- **多 LLM 支持** — 兼容 OpenAI、阿里通义、DeepSeek 等 OpenAI 兼容接口
- **API 文档** — 内置 Swagger UI，接口一目了然
- **前后端分离** — 清晰的架构，易于扩展

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
│   │   └── ChatController.java             # REST API 控制器
│   ├── 📂 model/
│   │   ├── ChatRequest.java                # 请求 DTO
│   │   └── ChatResponse.java               # 响应 DTO
│   └── 📂 service/
│       ├── ChatService.java                # 基础聊天服务
│       └── ConversationService.java        # 多轮对话服务（含流式）
│
├── 📂 src/main/resources/
│   ├── application.yml                     # 应用配置
│   └── 📂 static/                          # 前端构建产物
│
├── 📂 frontend/                            # Vue 3 前端源码
│   ├── 📂 src/
│   │   ├── App.vue                         # 根组件
│   │   ├── 📂 api/
│   │   │   └── chat.js                     # API 封装（含 SSE 处理）
│   │   ├── 📂 components/
│   │   │   ├── ChatHeader.vue              # 头部导航
│   │   │   ├── ChatInput.vue               # 输入组件
│   │   │   ├── ChatMessages.vue            # 消息列表
│   │   │   └── MessageBubble.vue           # 消息气泡（Markdown 渲染）
│   │   └── 📂 styles/
│   │       └── main.scss                   # 全局样式
│   └── package.json
│
└── pom.xml                                 # Maven 配置
```

---

## 📖 API 文档

### 接口概览

| 方法 | 路径 | 描述 |
|------|------|------|
| `POST` | `/api/chat/simple` | 简单对话（单轮） |
| `POST` | `/api/chat/with-context` | 带系统提示词的对话 |
| `POST` | `/api/chat/conversation` | 多轮对话（保持上下文） |
| `POST` | `/api/chat/conversation/stream` | 流式多轮对话（SSE） |
| `DELETE` | `/api/chat/conversation/{sessionId}` | 清除会话记忆 |
| `GET` | `/api/chat/health` | 健康检查 |

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
| LangChain4j | 0.35.0 | LLM 编排框架 |
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

## 🤝 贡献

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
