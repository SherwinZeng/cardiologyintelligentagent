<script setup lang="ts">
import { ChatDotRound, FirstAidKit, Warning } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'

import logoUrl from '@/assets/brand/logo.png'
import mingmingLoginUrl from '@/assets/character/mingming-login.png'

const { t } = useI18n()

const features = [
  { key: 'consult', icon: ChatDotRound },
  { key: 'urgency', icon: Warning },
  { key: 'care', icon: FirstAidKit },
] as const
</script>

<template>
  <aside class="login-page__hero" aria-hidden="true">
    <div class="login-page__hero-bg" />

    <div class="login-page__hero-brand">
      <img class="login-page__hero-logo" :src="logoUrl" :alt="t('app.name')" />
      <div class="login-page__hero-brand-text">
        <h1 class="login-page__hero-title">{{ t('app.name') }}</h1>
        <p class="login-page__hero-tagline">{{ t('app.tagline') }}</p>
      </div>
    </div>

    <div class="login-page__hero-stage">
      <div class="login-page__hero-character-shell">
        <div class="login-page__hero-character-wrap">
          <div class="login-page__hero-character-aura" aria-hidden="true" />
          <img
            class="login-page__hero-character"
            :src="mingmingLoginUrl"
            alt=""
          />
        </div>
      </div>
    </div>

    <ul class="login-page__hero-features">
      <li v-for="item in features" :key="item.key" class="login-page__hero-feature">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ t(`feat.${item.key}`) }}</span>
      </li>
    </ul>
  </aside>
</template>

<style scoped lang="scss">
@use '../styles/login-hero.scss';
</style>
