<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import mingmingWelcomeUrl from '@/assets/character/mingming-welcome-q.png';
import { readI18nSections } from '@/utils/i18nSections';

const { t, tm } = useI18n();
const router = useRouter();

const sections = computed(() => readI18nSections(tm('recordsPage.sections')));

function handleGoChat() {
  void router.push({ name: 'chat' });
}
</script>

<template>
  <section class="records-page">
    <div class="records-page__hero">
      <img class="records-page__avatar" :src="mingmingWelcomeUrl" :alt="t('ai.assistant')" />
      <div class="records-page__hero-text">
        <h1 class="records-page__title">{{ t('recordsPage.title') }}</h1>
        <p class="records-page__subtitle">{{ t('recordsPage.subtitle') }}</p>
      </div>
    </div>

    <div class="records-page__sections">
      <article v-for="(section, index) in sections" :key="index" class="records-page__section">
        <h2 class="records-page__section-title">{{ section.title }}</h2>
        <ul class="records-page__list">
          <li v-for="(item, itemIndex) in section.items" :key="itemIndex">
            {{ item }}
          </li>
        </ul>
      </article>
    </div>

    <aside class="records-page__notice">
      <p>{{ t('recordsPage.notice') }}</p>
    </aside>

    <div class="records-page__actions">
      <el-button type="primary" @click="handleGoChat">
        {{ t('recordsPage.goChat') }}
      </el-button>
    </div>
  </section>
</template>

<style scoped lang="scss">
@use './styles/records-view.scss';
</style>
