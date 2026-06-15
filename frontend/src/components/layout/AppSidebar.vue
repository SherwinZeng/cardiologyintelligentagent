<script setup lang="ts">
import {
  AlarmClock,
  DataAnalysis,
  Document,
  HomeFilled,
  Notebook,
  Setting,
  TrendCharts,
} from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

import PrivacyShieldIcon from '@/components/icons/PrivacyShieldIcon.vue'

const { t } = useI18n()
const route = useRoute()

interface SidebarItem {
  icon: typeof HomeFilled
  labelKey: string
  to?: string
  name?: string
  disabled?: boolean
}

const items: SidebarItem[] = [
  { icon: HomeFilled, labelKey: 'nav.home', to: '/', name: 'home' },
  { icon: Document, labelKey: 'nav.records', to: '/records', name: 'records' },
  { icon: DataAnalysis, labelKey: 'nav.reports', to: '/reports', name: 'reports' },
  { icon: TrendCharts, labelKey: 'nav.healthData', disabled: true },
  { icon: Notebook, labelKey: 'nav.healthKnowledge', disabled: true },
  { icon: AlarmClock, labelKey: 'nav.followUp', disabled: true },
  { icon: Setting, labelKey: 'nav.settings', disabled: true },
]

function isActive(name?: string) {
  return Boolean(name && route.name === name)
}
</script>

<template>
  <aside class="app-sidebar" aria-label="sidebar">
    <nav class="app-sidebar__nav">
      <component
        :is="item.disabled ? 'button' : 'router-link'"
        v-for="(item, index) in items"
        :key="index"
        :to="item.disabled ? undefined : item.to"
        type="button"
        class="app-sidebar__item"
        :class="{
          'is-active': isActive(item.name),
          'is-disabled': item.disabled,
        }"
        :disabled="item.disabled"
      >
        <el-icon :size="18">
          <component :is="item.icon" />
        </el-icon>
        <span>{{ t(item.labelKey) }}</span>
      </component>
    </nav>

    <div class="app-sidebar__promo">
      <PrivacyShieldIcon class="app-sidebar__promo-icon" />
      <p>{{ t('sidebar.promo') }}</p>
    </div>
  </aside>
</template>

<style scoped lang="scss">
@use './styles/app-sidebar.scss';
</style>
