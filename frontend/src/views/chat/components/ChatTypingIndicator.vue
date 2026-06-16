<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import mingmingChatAvatarUrl from '@/assets/character/mingming-welcome-q.png'

const { t } = useI18n()
</script>

<template>
  <article class="chat-message is-assistant chat-message--typing" aria-live="polite">
    <el-avatar
      class="chat-message__avatar chat-message__avatar--assistant"
      :size="40"
      :src="mingmingChatAvatarUrl"
    />

    <div class="chat-message__body">
      <header class="chat-message__meta">
        <span class="chat-message__name">{{ t('ai.assistant') }}</span>
      </header>

      <div class="chat-message__bubble chat-message__bubble--typing">
        <span class="chat-message__typing-dots" aria-hidden="true">
          <span />
          <span />
          <span />
        </span>
        <span class="chat-message__typing-text">{{ t('chat.typing') }}</span>
      </div>
    </div>
  </article>
</template>

<style scoped lang="scss">
@use '../styles/chat-message.scss';
</style>
