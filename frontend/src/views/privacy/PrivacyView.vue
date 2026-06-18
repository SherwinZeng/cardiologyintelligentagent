<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

import PrivacyShieldIcon from '@/components/icons/PrivacyShieldIcon.vue';

interface PrivacySection {
  title: string;
  items: string[];
}

const { t, tm } = useI18n();

const sections = computed(() => tm('privacyPage.sections') as PrivacySection[]);
</script>

<template>
  <section class="privacy-page">
    <div class="privacy-page__hero">
      <div class="privacy-page__icon-wrap" aria-hidden="true">
        <PrivacyShieldIcon class="privacy-page__icon" />
      </div>
      <div class="privacy-page__hero-text">
        <h1 class="privacy-page__title">{{ t('privacyPage.title') }}</h1>
        <p class="privacy-page__subtitle">{{ t('privacyPage.subtitle') }}</p>
        <p class="privacy-page__updated">{{ t('privacyPage.lastUpdated') }}</p>
      </div>
    </div>

    <div class="privacy-page__sections">
      <article
        v-for="(section, index) in sections"
        :key="index"
        class="privacy-page__section"
        :class="{ 'privacy-page__section--highlight': index === 2 }"
      >
        <h2 class="privacy-page__section-title">{{ section.title }}</h2>
        <ul class="privacy-page__list">
          <li v-for="(item, itemIndex) in section.items" :key="itemIndex">
            {{ item }}
          </li>
        </ul>
      </article>
    </div>

    <aside class="privacy-page__notice">
      <p>{{ t('privacyPage.notice') }}</p>
    </aside>
  </section>
</template>

<style scoped lang="scss">
@use './styles/privacy-view.scss';
</style>
