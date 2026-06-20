<script setup lang="ts">
import { computed, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useI18n } from 'vue-i18n';

import guestAvatarUrl from '@/assets/character/mingming-avatar.png';
import mingmingChatAvatarUrl from '@/assets/character/mingming-welcome-q.png';
import { useTypewriter } from '@/hooks/useTypewriter';
import { useUserLoginStore } from '@/stores/login.ts';

import type { ChatMessage } from '../types';

const props = defineProps<{
  message: ChatMessage;
}>();

const emit = defineEmits<{
  'typing-tick': [];
  'typing-complete': [];
}>();

const { t } = useI18n();
const { displayName } = storeToRefs(useUserLoginStore());
const { displayedText, isTyping, start, skip } = useTypewriter({ speed: 20 });

const isAssistant = computed(() => props.message.role === 'assistant');
const shouldAnimate = computed(() => isAssistant.value && props.message.animate === true);

const hasSectionContent = computed(() =>
  Boolean(
    props.message.sections?.analysis ||
      props.message.sections?.advice ||
      props.message.sections?.notes,
  ),
);

const showSections = computed(() => {
  if (!isAssistant.value || !hasSectionContent.value) {
    return false;
  }
  // 主回复打字机未结束时，不展示就医建议 / 注意事项等附加块
  if (shouldAnimate.value && isTyping.value) {
    return false;
  }
  return true;
});

const bubbleText = computed(() => {
  if (!isAssistant.value) {
    return props.message.content;
  }
  if (shouldAnimate.value) {
    return displayedText.value;
  }
  return props.message.content;
});

watch(
  () => [props.message.id, props.message.content, props.message.animate, props.message.typing] as const,
  ([, content, animate, typing]) => {
    if (!isAssistant.value || typing) {
      return;
    }
    if (animate) {
      start(content, () => emit('typing-tick'));
      return;
    }
    skip(content);
  },
  { immediate: true },
);

watch(isTyping, (typing, wasTyping) => {
  if (wasTyping && !typing && shouldAnimate.value) {
    emit('typing-complete');
  }
});
</script>

<template>
  <article
    v-if="message.typing"
    class="chat-message is-assistant chat-message--typing"
    aria-live="polite"
  >
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

  <article v-else class="chat-message" :class="message.role === 'user' ? 'is-user' : 'is-assistant'">
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
          {{ message.role === 'assistant' ? t('ai.assistant') : displayName }}
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
        <p class="chat-message__text">
          {{ bubbleText
          }}<span
            v-if="shouldAnimate && isTyping"
            class="chat-message__cursor"
            aria-hidden="true"
          />
        </p>

        <div v-if="showSections" class="chat-message__sections">
          <section v-if="message.sections?.analysis" class="chat-message__section">
            <h4>{{ t('ai.analysis') }}</h4>
            <p>{{ message.sections.analysis }}</p>
          </section>
          <section v-if="message.sections?.advice" class="chat-message__section">
            <h4>{{ t('ai.advice') }}</h4>
            <p>{{ message.sections.advice }}</p>
          </section>
          <section v-if="message.sections?.notes" class="chat-message__section">
            <h4>{{ t('ai.notes') }}</h4>
            <p>{{ message.sections.notes }}</p>
          </section>
        </div>
      </div>

      <p v-if="showSections && message.role === 'assistant'" class="chat-message__disclaimer">
        {{ t('ai.disclaimer') }}
      </p>
      <p
        v-if="showSections && message.role === 'assistant' && message.guideReferences?.length"
        class="chat-message__guide-refs"
      >
        {{ t('ai.guideReferences') }}：{{ message.guideReferences.join('、') }}
      </p>
    </div>
  </article>
</template>

<style scoped lang="scss">
@use '../styles/chat-message.scss';
</style>
