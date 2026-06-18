<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';

import ChatSessionSwipeItem from './ChatSessionSwipeItem.vue';
import type { ChatSessionItem } from '../types';

const SESSION_PAGER_COUNT = 5;

defineProps<{
  sessions: ChatSessionItem[];
  activeSessionId: string;
  loading?: boolean;
  page: number;
  pageSize: number;
  total: number;
  /** 在窄屏也展示会话列表（问诊记录页需要） */
  mobileVisible?: boolean;
}>();

const emit = defineEmits<{
  select: [sessionId: string];
  new: [];
  search: [keyword: string];
  pageChange: [page: number];
  delete: [sessionId: string];
  pin: [sessionId: string, pinned: boolean];
}>();

const { t } = useI18n();
const openedSessionId = ref('');
const searchInput = ref('');

function handleOpenChange(sessionId: string, open: boolean) {
  openedSessionId.value = open ? sessionId : '';
}

function closeSwipeActions() {
  openedSessionId.value = '';
}

function handleSearchInput(event: Event) {
  const value = (event.target as HTMLInputElement).value;
  searchInput.value = value;
  emit('search', value);
}

function handleListClick() {
  closeSwipeActions();
}
</script>

<template>
  <aside
    class="chat-session-aside"
    :class="{ 'is-mobile-visible': mobileVisible }"
    aria-label="session list"
  >
    <div class="chat-session-aside__toolbar">
      <el-button type="primary" class="chat-session-aside__new" @click="emit('new')">
        <el-icon><Plus /></el-icon>
        {{ t('chat.new') }}
      </el-button>
      <div class="chat-session-aside__search">
        <el-icon class="chat-session-aside__search-icon"><Search /></el-icon>
        <input
          v-model="searchInput"
          class="chat-session-aside__search-input"
          type="search"
          :placeholder="t('records.search')"
          @input="handleSearchInput"
        />
      </div>
    </div>

    <p class="chat-session-aside__label">{{ t('chat.recent') }}</p>

    <div v-loading="loading" class="chat-session-aside__list-wrap" @click="handleListClick">
      <ul v-if="sessions.length > 0" class="chat-session-aside__list">
        <ChatSessionSwipeItem
          v-for="session in sessions"
          :key="session.id"
          :session="session"
          :active="session.id === activeSessionId"
          :open="openedSessionId === session.id"
          @select="emit('select', session.id)"
          @delete="emit('delete', session.id)"
          @pin="emit('pin', session.id, !session.pinned)"
          @open-change="handleOpenChange(session.id, $event)"
        />
      </ul>
      <p v-else class="chat-session-aside__empty">
        {{ searchInput.trim() ? t('chat.searchEmpty') : t('records.listEmpty') }}
      </p>

      <el-pagination
        v-if="total > pageSize"
        class="chat-session-aside__pagination"
        small
        background
        layout="prev, pager, next"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        :pager-count="SESSION_PAGER_COUNT"
        @current-change="emit('pageChange', $event)"
      />
    </div>
  </aside>
</template>

<style scoped lang="scss">
@use '../styles/chat-session-aside.scss';
</style>
