import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import './styles/main.scss'

// 导入代码高亮样式
import 'highlight.js/styles/github-dark.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.mount('#app')
