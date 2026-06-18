<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

import { env } from '@/config/env';

const { t } = useI18n();

const copyrightText = computed(() =>
  t('footer.copyright', {
    year: new Date().getFullYear(),
    owner: env.copyrightOwner,
  }),
);

const hasIcp = computed(() => Boolean(env.icpNumber));
const hasPsb = computed(() => Boolean(env.psbNumber));
</script>

<template>
  <footer class="app-footer">
    <div class="app-footer__inner">
      <a
        v-if="hasIcp"
        class="app-footer__link"
        :href="env.icpLink"
        target="_blank"
        rel="noopener noreferrer"
      >
        {{ env.icpNumber }}
      </a>

      <span v-if="hasIcp && hasPsb" class="app-footer__sep" aria-hidden="true">|</span>

      <a
        v-if="hasPsb"
        class="app-footer__link"
        :href="env.psbLink"
        target="_blank"
        rel="noopener noreferrer"
      >
        {{ env.psbNumber }}
      </a>

      <span v-if="hasIcp || hasPsb" class="app-footer__sep" aria-hidden="true"> | </span>

      <span class="app-footer__copy">{{ copyrightText }}</span>
    </div>
  </footer>
</template>

<style scoped lang="scss">
@use './styles/app-footer.scss';
</style>
