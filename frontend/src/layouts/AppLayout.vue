<script setup lang="ts">
import { computed, onMounted } from 'vue'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import AppFooter from '@/components/layout/AppFooter.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import { useLocaleStore } from '@/stores/locale'
import { useThemeStore } from '@/stores/theme'

const localeStore = useLocaleStore()
const themeStore = useThemeStore()

const elementLocale = computed(() =>
  localeStore.locale === 'zh-CN' ? zhCn : en,
)

onMounted(() => {
  themeStore.init()
})
</script>

<template>
  <el-config-provider :locale="elementLocale">
    <div class="app-layout">
      <AppHeader />
      <div class="app-layout__body">
        <AppSidebar />
        <div class="app-layout__content">
          <main class="app-layout__main">
            <router-view />
          </main>
          <AppFooter />
        </div>
      </div>
    </div>
  </el-config-provider>
</template>

<style scoped lang="scss">
@use './styles/app-layout.scss';
</style>
