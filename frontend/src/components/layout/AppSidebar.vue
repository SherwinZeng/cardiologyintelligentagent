<script setup lang="ts">
import {
  Calendar,
  ChatDotRound,
  DataAnalysis,
  Document,
  Notebook,
  TrendCharts,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

import logoUrl from '@/assets/brand/logo.png'
import PrivacyShieldIcon from '@/components/icons/PrivacyShieldIcon.vue'
import { useLocaleStore } from '@/stores/locale'

const { t } = useI18n()
const route = useRoute()
const localeStore = useLocaleStore()

const isZh = computed(() => localeStore.locale === 'zh-CN')

interface SidebarItem {
  icon?: Component
  labelKey?: string
  to?: string
  name?: string
  disabled?: boolean
  logoOnly?: boolean
}

const items: SidebarItem[] = [
  { logoOnly: true, to: '/', name: 'home' },
  { icon: ChatDotRound, labelKey: 'nav.consult', to: '/chat', name: 'chat' },
  { icon: Document, labelKey: 'nav.records', to: '/records', name: 'records' },
  { icon: DataAnalysis, labelKey: 'nav.reports', to: '/reports', name: 'reports' },
  { icon: Calendar, labelKey: 'nav.appointment', to: '/appointment', name: 'appointment' },
  { icon: TrendCharts, labelKey: 'nav.healthData', disabled: true },
  { icon: Notebook, labelKey: 'nav.healthKnowledge', disabled: true },
]

function isActive(name?: string) {
  return Boolean(name && route.name === name)
}
</script>

<template>
  <aside
    class="app-sidebar"
    :class="{ 'is-locale-zh': isZh, 'is-locale-en': !isZh }"
    aria-label="sidebar"
  >
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
          'is-logo-only': item.logoOnly,
        }"
        :disabled="item.disabled"
        :aria-label="item.logoOnly ? t('nav.home') : undefined"
      >
        <span class="app-sidebar__icon-wrap">
          <img
            v-if="item.logoOnly"
            class="app-sidebar__logo"
            :src="logoUrl"
            :alt="t('app.name')"
          />
          <el-icon v-else :size="22">
            <component :is="item.icon" />
          </el-icon>
        </span>
        <span v-if="!item.logoOnly && item.labelKey" class="app-sidebar__label">
          {{ t(item.labelKey) }}
        </span>
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
