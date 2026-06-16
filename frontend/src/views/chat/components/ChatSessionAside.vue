<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'

import type { ChatSessionItem } from '../types'

defineProps<{
  sessions: ChatSessionItem[]
  activeSessionId: string
}>()

/** 会话切换、新建问诊、搜索关键词交给 ChatView / useChat 处理 */
const emit = defineEmits<{
  select: [sessionId: string]
  new: []
  search: [keyword: string]
}>()

const { t } = useI18n()
</script>

<template>
  <aside class="chat-session-aside" aria-label="session list">
    <div class="chat-session-aside__toolbar">
      <el-button type="primary" class="chat-session-aside__new" @click="emit('new')">
        <el-icon><Plus /></el-icon>
        {{ t('chat.new') }}
      </el-button>
      <div class="chat-session-aside__search">
        <el-icon class="chat-session-aside__search-icon"><Search /></el-icon>
        <input
          class="chat-session-aside__search-input"
          type="search"
          :placeholder="t('records.search')"
          @input="emit('search', ($event.target as HTMLInputElement).value)"
        />
      </div>
    </div>

    <p class="chat-session-aside__label">{{ t('chat.recent') }}</p>

    <ul v-if="sessions.length > 0" class="chat-session-aside__list">
      <li v-for="session in sessions" :key="session.id">
        <button
          type="button"
          class="chat-session-aside__item"
          :class="{ 'is-active': session.id === activeSessionId }"
          @click="emit('select', session.id)"
        >
          <div class="chat-session-aside__item-head">
            <span class="chat-session-aside__item-title">{{ session.title }}</span>
            <time class="chat-session-aside__item-time">{{ session.updatedAt }}</time>
          </div>
          <p class="chat-session-aside__item-preview">
            {{ session.preview || '暂无消息' }}
          </p>
          <div class="chat-session-aside__item-foot">
            <span class="chat-session-aside__item-meta">
              {{ t('records.messages', { n: session.messageCount }) }}
            </span>
            <span
              v-if="session.status"
              class="chat-session-aside__item-status"
              :class="`is-${session.status}`"
            >
              {{ session.status === 'active' ? '进行中' : session.status }}
            </span>
          </div>
        </button>
      </li>
    </ul>
    <p v-else class="chat-session-aside__empty">{{ t('records.listEmpty') }}</p>
  </aside>
</template>

<style scoped lang="scss">
@use '../styles/chat-session-aside.scss';
</style>
