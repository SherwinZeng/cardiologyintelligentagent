<script setup lang="ts">
import { ArrowDown, Bell, QuestionFilled } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'

import LocaleSwitch from '@/components/common/LocaleSwitch.vue'
import ThemeSwitch from '@/components/common/ThemeSwitch.vue'
import PrivacyShieldIcon from '@/components/icons/PrivacyShieldIcon.vue'
import logoUrl from '@/assets/brand/logo.png'
import mingmingAvatarUrl from '@/assets/character/mingming-avatar.png'

const { t } = useI18n()
</script>

<template>
  <header class="app-header">
    <div class="app-header__brand">
      <img class="app-header__logo" :src="logoUrl" :alt="t('app.name')" />
      <span class="app-header__name">{{ t('app.name') }}</span>
    </div>

    <div class="app-header__actions">
      <button type="button" class="app-header__link">
        <PrivacyShieldIcon class="app-header__link-icon app-header__link-icon--privacy" />
        <span class="app-header__link-text">{{ t('nav.privacy') }}</span>
      </button>
      <button type="button" class="app-header__link">
        <el-icon class="app-header__link-icon"><QuestionFilled /></el-icon>
        <span class="app-header__link-text">{{ t('nav.help') }}</span>
      </button>
      <LocaleSwitch class="app-header__tool" />
      <ThemeSwitch class="app-header__tool" />
      <el-badge :value="3" class="app-header__badge">
        <el-button circle class="app-header__icon-btn" :aria-label="'notifications'">
          <el-icon><Bell /></el-icon>
        </el-button>
      </el-badge>
      <button type="button" class="app-header__user" aria-label="user menu">
        <el-avatar :size="32" :src="mingmingAvatarUrl" />
        <span class="app-header__username">{{ t('user.guest') }}</span>
        <el-icon><ArrowDown /></el-icon>
      </button>
    </div>
  </header>
</template>

<style scoped lang="scss">
@use './styles/app-header.scss';
</style>
