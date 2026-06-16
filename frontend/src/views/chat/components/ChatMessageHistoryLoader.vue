<script setup lang="ts">
import MingmingLoadingTipContent from '@/components/common/MingmingLoadingTipContent.vue'
import { useI18n } from 'vue-i18n'

defineProps<{
  text?: string
}>()

const { t } = useI18n()
</script>

<template>
  <div class="chat-message-history-loader" role="status" aria-live="polite">
    <MingmingLoadingTipContent :text="text || t('chat.loadingOlderMessages')" />
  </div>
</template>

<style scoped lang="scss">
.chat-message-history-loader {
  display: flex;
  justify-content: center;
  padding: 8px 0 12px;
}
</style>
