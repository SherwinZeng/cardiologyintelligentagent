<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import guestAvatarUrl from '@/assets/character/mingming-avatar.png'
import mingmingChatAvatarUrl from '@/assets/character/mingming-welcome-q.png'

import type { ChatMessage } from '../types'

defineProps<{
  message: ChatMessage
}>()

const { t } = useI18n()
</script>

<template>
  <article
    class="chat-message"
    :class="message.role === 'user' ? 'is-user' : 'is-assistant'"
  >
    <el-avatar
      v-if="message.role === 'assistant'"
      class="chat-message__avatar chat-message__avatar--assistant"
      :size="40"
      :src="mingmingChatAvatarUrl"
    />
    <el-avatar
      v-else
      class="chat-message__avatar chat-message__avatar--user"
      :size="40"
      :src="guestAvatarUrl"
    />

    <div class="chat-message__body">
      <header class="chat-message__meta">
        <span class="chat-message__name">
          {{ message.role === 'assistant' ? t('ai.assistant') : t('user.guest') }}
        </span>
        <span
          v-if="message.role === 'assistant' && message.urgency"
          class="chat-message__urgency"
          :class="`is-${message.urgency}`"
        >
          {{ t(`urgency.${message.urgency}`) }}
        </span>
        <time class="chat-message__time">{{ message.time }}</time>
      </header>

      <div class="chat-message__bubble">
        <p class="chat-message__text">{{ message.content }}</p>

        <div v-if="message.sections" class="chat-message__sections">
          <section v-if="message.sections.analysis" class="chat-message__section">
            <h4>{{ t('ai.analysis') }}</h4>
            <p>{{ message.sections.analysis }}</p>
          </section>
          <section v-if="message.sections.advice" class="chat-message__section">
            <h4>{{ t('ai.advice') }}</h4>
            <p>{{ message.sections.advice }}</p>
          </section>
          <section v-if="message.sections.notes" class="chat-message__section">
            <h4>{{ t('ai.notes') }}</h4>
            <p>{{ message.sections.notes }}</p>
          </section>
        </div>
      </div>

      <p v-if="message.role === 'assistant' && message.sections" class="chat-message__disclaimer">
        {{ t('ai.disclaimer') }}
      </p>
    </div>
  </article>
</template>

<style scoped lang="scss">
@use '../styles/chat-message.scss';
</style>
