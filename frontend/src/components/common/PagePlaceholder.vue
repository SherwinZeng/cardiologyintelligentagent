<script setup lang="ts">
import { useI18n } from 'vue-i18n';

defineProps<{
  titleKey: string;
}>();

const { t } = useI18n();
</script>

<template>
  <section class="page-placeholder">
    <div class="page-placeholder__card">
      <h1 class="page-placeholder__title">{{ t(titleKey) }}</h1>
      <p class="page-placeholder__desc">{{ t('app.tagline') }}</p>
    </div>
  </section>
</template>

<style scoped lang="scss">
@use './styles/page-placeholder.scss';
</style>
