import { createPinia } from 'pinia'
import { createApp } from 'vue'

import '@/config/env'
import App from './App.vue'
import i18n from './i18n'
import router from './router'
import { useLocaleStore } from './stores/locale'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './styles/index.scss'

const pinia = createPinia()
const app = createApp(App)

app.use(pinia)
app.use(i18n)
app.use(router)

useLocaleStore(pinia).init()

app.mount('#app')
