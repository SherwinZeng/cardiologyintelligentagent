<script setup lang="ts">
import mingmingQAvatarUrl from '@/assets/character/mingming-welcome-q.png';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

const props = defineProps<{
  text?: string;
}>();

const { t } = useI18n();

const displayText = computed(() => props.text?.trim() || t('sidebar.loadingTip'));
</script>

<template>
  <div class="mingming-loading-tip__content">
    <img
      class="mingming-loading-tip__avatar"
      :src="mingmingQAvatarUrl"
      :alt="t('chat.assistant')"
    />
    <span class="mingming-loading-tip__text">{{ displayText }}</span>
  </div>
</template>
