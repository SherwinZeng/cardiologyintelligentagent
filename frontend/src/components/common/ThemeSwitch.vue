<script setup lang="ts">
import { Moon, Sunny } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import { useThemeStore, type ThemePreference } from '@/stores/theme'

const { t } = useI18n()
const themeStore = useThemeStore()
const { preference } = storeToRefs(themeStore)

const options: ThemePreference[] = ['light', 'dark', 'system']

const activeIcon = computed(() => (themeStore.resolvedTheme === 'dark' ? Moon : Sunny))

function label(mode: ThemePreference) {
  return t(`theme.${mode}`)
}
</script>

<template>
  <el-dropdown trigger="click" @command="themeStore.applyPreference">
    <el-button circle :aria-label="t('theme.label')">
      <el-icon>
        <component :is="activeIcon" />
      </el-icon>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="item in options"
          :key="item"
          :command="item"
          :class="{ 'is-active': preference === item }"
        >
          {{ label(item) }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped lang="scss">
@use './styles/theme-switch.scss';
</style>
