<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import { SUPPORT_LOCALES, type AppLocale } from '@/i18n'
import { useLocaleStore } from '@/stores/locale'

const { t, te } = useI18n()
const localeStore = useLocaleStore()
const { locale } = storeToRefs(localeStore)

const localeLabel = computed(() => (locale.value === 'zh-CN' ? '中' : 'EN'))

function localeOptionLabel(item: AppLocale) {
  const key = `locale.${item}`
  return te(key) ? t(key) : item
}
</script>

<template>
  <el-dropdown trigger="click" @command="localeStore.applyLocale">
    <el-button circle class="locale-switch" :aria-label="t('locale.label')">
      <span class="locale-switch__text">{{ localeLabel }}</span>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="item in SUPPORT_LOCALES"
          :key="item"
          :command="item"
          :class="{ 'is-active': locale === item }"
        >
          {{ localeOptionLabel(item) }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped lang="scss">
@use './styles/locale-switch.scss';
</style>
