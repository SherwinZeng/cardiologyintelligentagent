<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

const { t } = useI18n()
const route = useRoute()

const draftMessage = computed(() => {
  const value = route.query.message
  return typeof value === 'string' ? value : ''
})
</script>

<template>
  <section class="chat-placeholder">
    <div class="chat-placeholder__card">
      <h1 class="chat-placeholder__title">{{ t('chat.with') }}</h1>
      <p v-if="draftMessage" class="chat-placeholder__message">{{ draftMessage }}</p>
      <p v-else class="chat-placeholder__hint">{{ t('chat.input') }}</p>
    </div>
  </section>
</template>

<style scoped lang="scss">
@use './styles/chat-view.scss';
</style>
