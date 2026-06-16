<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

import mingmingWelcomeUrl from '@/assets/character/mingming-welcome-q.png'

interface HelpSection {
  title: string
  items: string[]
}

const { t, tm } = useI18n()
const router = useRouter()

const sections = computed(() => tm('helpPage.sections') as HelpSection[])

function handleStartChat() {
  void router.push({ name: 'chat' })
}
</script>

<template>
  <section class="help-page">
    <div class="help-page__hero">
      <img
        class="help-page__avatar"
        :src="mingmingWelcomeUrl"
        :alt="t('ai.assistant')"
      />
      <div class="help-page__hero-text">
        <h1 class="help-page__title">{{ t('helpPage.title') }}</h1>
        <p class="help-page__subtitle">{{ t('helpPage.subtitle') }}</p>
      </div>
    </div>

    <div class="help-page__sections">
      <article
        v-for="(section, index) in sections"
        :key="index"
        class="help-page__section"
      >
        <h2 class="help-page__section-title">{{ section.title }}</h2>
        <ul class="help-page__list">
          <li v-for="(item, itemIndex) in section.items" :key="itemIndex">
            {{ item }}
          </li>
        </ul>
      </article>
    </div>

    <aside class="help-page__notice">
      <p>{{ t('helpPage.notice') }}</p>
    </aside>

    <div class="help-page__actions">
      <el-button type="primary" @click="handleStartChat">
        {{ t('helpPage.startChat') }}
      </el-button>
    </div>
  </section>
</template>

<style scoped lang="scss">
@use './styles/help-view.scss';
</style>
