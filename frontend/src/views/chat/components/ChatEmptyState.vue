<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import mingmingExplainingUrl from '@/assets/character/mingming-explaining.png'
import CardioDecorLayer from '@/components/common/CardioDecorLayer.vue'

const { t } = useI18n()
</script>

<template>
  <div class="chat-empty">
    <CardioDecorLayer />
    <div class="chat-empty__hero">
      <img
        class="chat-empty__photo"
        :src="mingmingExplainingUrl"
        :alt="t('ai.assistant')"
      />
      <div class="chat-empty__caption">
        <i18n-t keypath="chat.emptyInvite" tag="p" class="chat-empty__hint">
          <template #name>
            <span class="chat-empty__hint-accent">{{ t('ai.name') }}</span>
          </template>
        </i18n-t>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use './styles/chat-empty-state.scss';
</style>
