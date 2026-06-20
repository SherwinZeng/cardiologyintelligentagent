<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { v4 as uuidV4 } from 'uuid';
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

import { getApiErrorMessage } from '@/http';
import LoginPromptDialog from '@/components/auth/LoginPromptDialog.vue';
import MingmingLoadingTipContent from '@/components/common/MingmingLoadingTipContent.vue';
import { deleteSessionService } from '@/services/chat/deleteSession.ts';
import { fetchMessagesService } from '@/services/chat/fetchMessages.ts';
import { fetchSessionsService } from '@/services/chat/fetchSessions.ts';
import { handleGeneralUnderstandingService } from '@/services/chat/handleGeneralUnderstanding.ts';
import { pinSessionService } from '@/services/chat/pinSession.ts';
import {
  mapUrgencyLevel,
  parseMessagePageData,
  parseSessionPageData,
  toChatMessageList,
  toSessionItem,
  toSessionItemList,
} from '@/services/chat/mapChatMessage.ts';
import { handleCreatSessionService } from '@/services/chat/handleSession.ts';
import { useUserLoginStore } from '@/stores/login.ts';
import { useEnsureAuthWithPrompt } from '@/hooks/useEnsureAuthWithPrompt.ts';
import { useRequireAuth } from '@/hooks/useRequireAuth.ts';
import type { IBaseResponse } from '@/typings/baseResponse.ts';
import type {
  IChatMessagePageResponse,
  IGeneralUnderstandingResponse,
} from '@/typings/chat/message.ts';
import type { IChatSessionPageResponse, ICreateSessionResponse } from '@/typings/chat/session.ts';

import ChatComposer from './components/ChatComposer.vue';
import ChatEmptyState from './components/ChatEmptyState.vue';
import ChatMessageBubble from './components/ChatMessageBubble.vue';
import ChatMessageHistoryLoader from './components/ChatMessageHistoryLoader.vue';
import ChatSessionAside from './components/ChatSessionAside.vue';
import type { ChatMessage, ChatSessionItem } from './types';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const { userLoginStore } = useUserLoginStore();
const { requireAuth } = useRequireAuth();
const { ensureAuthWithPrompt } = useEnsureAuthWithPrompt();

const SESSION_PAGE_SIZE = 5;
const SESSION_SEARCH_DEBOUNCE_MS = 300;
const MESSAGE_PAGE_SIZE = 40;
const MESSAGE_SCROLL_TOP_THRESHOLD = 48;

const sessions = ref<ChatSessionItem[]>([]);
const searchKeyword = ref('');
const sessionPage = ref(1);
const sessionTotal = ref(0);
const messages = ref<ChatMessage[]>([]);
const messagesHasMore = ref(false);
const activeSessionId = ref('');
const inflightSessionIds = ref<Set<string>>(new Set());
/** 切走 session 时 API 未返回，切回来需恢复用户消息 + 打字占位 */
const pendingOutboundBySession = ref<
  Record<string, { userMessage: ChatMessage; typingPlaceholderId: string }>
>({});
const loadingMessages = ref(false);
const loadingOlderMessages = ref(false);
const loadingSessions = ref(false);
const suppressAutoScroll = ref(false);
const stickToBottom = ref(true);
const canLoadOlderMessages = ref(false);
const messagesContainerRef = ref<HTMLElement | null>(null);
const chatComposerRef = ref<InstanceType<typeof ChatComposer> | null>(null);
let sessionSearchTimer: ReturnType<typeof setTimeout> | null = null;
let programmaticScroll = false;

const draftMessage = computed(() => {
  const value = route.query.message;
  return typeof value === 'string' ? value : '';
});

const currentTopic = computed(
  () => sessions.value.find((item) => item.id === activeSessionId.value)?.title ?? '',
);

/** 仅当前会话在等 AI 回复时禁用发送，其它会话可正常输入 */
const sending = computed(() => inflightSessionIds.value.has(activeSessionId.value));

const showEmptyState = computed(
  () => messages.value.length === 0 && !loadingMessages.value && !sending.value,
);

function applyScrollToContainer(element: HTMLElement, force = false) {
  if (!force && !stickToBottom.value) {
    return;
  }
  programmaticScroll = true;
  element.scrollTop = element.scrollHeight;
  requestAnimationFrame(() => {
    programmaticScroll = false;
  });
}

function updateStickToBottom(container: HTMLElement) {
  if (programmaticScroll || suppressAutoScroll.value) {
    return;
  }
  stickToBottom.value = isNearBottom(container);
}

function shouldLoadOlderMessages(container: HTMLElement) {
  if (container.scrollHeight <= container.clientHeight + MESSAGE_SCROLL_TOP_THRESHOLD) {
    return false;
  }
  return container.scrollTop <= MESSAGE_SCROLL_TOP_THRESHOLD;
}

function isNearBottom(container: HTMLElement, threshold = 160) {
  return container.scrollHeight - container.scrollTop - container.clientHeight <= threshold
}

async function scrollToBottomIfNeeded() {
  if (suppressAutoScroll.value || !stickToBottom.value) {
    return;
  }
  const element = messagesContainerRef.value;
  if (!element) {
    return;
  }
  await scrollToBottom();
}

/** 打字机逐字输出时，仅当用户仍在底部附近才跟随滚动 */
let typewriterScrollRaf = 0;
function followTypewriterScroll() {
  if (suppressAutoScroll.value) {
    return;
  }
  if (typewriterScrollRaf !== 0) {
    return;
  }
  typewriterScrollRaf = window.requestAnimationFrame(() => {
    typewriterScrollRaf = 0;
    if (suppressAutoScroll.value || !stickToBottom.value) {
      return;
    }
    const element = messagesContainerRef.value;
    if (element) {
      applyScrollToContainer(element);
    }
  });
}

async function scrollToBottom() {
  if (suppressAutoScroll.value) {
    return;
  }

  stickToBottom.value = true;
  await nextTick();
  await new Promise<void>((resolve) => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        const element = messagesContainerRef.value;
        if (element) {
          applyScrollToContainer(element, true);
        }
        resolve();
      });
    });
  });
}

async function scrollMessagesToLatest() {
  canLoadOlderMessages.value = false;
  await scrollToBottom();
  const element = messagesContainerRef.value;
  if (element) {
    applyScrollToContainer(element, true);
  }
  canLoadOlderMessages.value = true;
}

function onMessagesLengthChange() {
  if (suppressAutoScroll.value || loadingOlderMessages.value || !stickToBottom.value) {
    return;
  }
  void scrollToBottomIfNeeded();
}

watch(() => messages.value.length, onMessagesLengthChange)

function markLastAssistantAnimated() {
  for (let index = messages.value.length - 1; index >= 0; index -= 1) {
    if (messages.value[index].role !== 'assistant') {
      continue;
    }
    messages.value[index] = { ...messages.value[index], animate: true };
    break;
  }
}

async function loadSessions(page = 1, silent = false) {
  if (!userLoginStore.id) {
    return
  }

  sessionPage.value = page
  if (!silent) {
    loadingSessions.value = true
  }

  try {
    const keyword = searchKeyword.value.trim();
    const response = await fetchSessionsService<IBaseResponse<IChatSessionPageResponse>>({
      uid: userLoginStore.id,
      page: sessionPage.value,
      pageSize: SESSION_PAGE_SIZE,
      keyword: keyword || undefined,
    });
    if (response.code !== 200 || !response.data) {
      ElMessage.error(response.message || t('chat.loadSessionsFailed'));
      return;
    }

    const pageData = parseSessionPageData(response.data);
    sessions.value = toSessionItemList(pageData.records);
    sessionTotal.value = pageData.total;
    sessionPage.value = pageData.page;
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error))
  } finally {
    if (!silent) {
      loadingSessions.value = false
    }
  }
}

function handleSessionPageChange(page: number) {
  void loadSessions(page);
}

function getOldestLoadedMessageId(): number | null {
  for (const message of messages.value) {
    const id = Number(message.id);
    if (Number.isFinite(id) && id > 0) {
      return id;
    }
  }
  return null;
}

function clearPendingOutbound(sessionId: string) {
  const next = { ...pendingOutboundBySession.value };
  delete next[sessionId];
  pendingOutboundBySession.value = next;
}

/** 从服务端拉历史后，补上尚未落库的乐观用户消息与 typing 占位 */
function applyPendingOutboundMessages(sessionId: string) {
  const pending = pendingOutboundBySession.value[sessionId];
  if (!pending) {
    return;
  }

  const serverHasUserMessage = messages.value.some(
    (message) =>
      message.role === 'user' && message.content.trim() === pending.userMessage.content.trim(),
  );
  if (!serverHasUserMessage) {
    messages.value.push(pending.userMessage);
  }

  const hasTyping = messages.value.some((message) => message.typing);
  if (!hasTyping && inflightSessionIds.value.has(sessionId)) {
    messages.value.push({
      id: pending.typingPlaceholderId,
      role: 'assistant',
      content: '',
      time: '',
      typing: true,
    });
  }
}

async function loadMessages(sessionId: string, animateLastAssistant = false) {
  if (!userLoginStore.id) {
    return;
  }

  canLoadOlderMessages.value = false;
  loadingMessages.value = true;
  messagesHasMore.value = false;
  try {
    const response = await fetchMessagesService<IBaseResponse<IChatMessagePageResponse>>({
      uid: userLoginStore.id,
      session: sessionId,
      pageSize: MESSAGE_PAGE_SIZE,
    });
    if (activeSessionId.value !== sessionId) {
      return;
    }
    if (response.code !== 200 || !response.data) {
      ElMessage.error(response.message || t('chat.loadFailed'));
      return;
    }

    const pageData = parseMessagePageData(response.data);
    messages.value = toChatMessageList(pageData.records);
    messagesHasMore.value = pageData.hasMore;
    applyPendingOutboundMessages(sessionId);

    if (animateLastAssistant) {
      markLastAssistantAnimated();
    }
  } catch (error) {
    if (activeSessionId.value === sessionId) {
      ElMessage.error(getApiErrorMessage(error));
    }
  } finally {
    if (activeSessionId.value === sessionId) {
      loadingMessages.value = false;
    }
  }

  if (activeSessionId.value !== sessionId) {
    return;
  }
  await scrollMessagesToLatest();
}

async function loadOlderMessages() {
  if (
    !userLoginStore.id ||
    !activeSessionId.value ||
    !messagesHasMore.value ||
    loadingMessages.value ||
    loadingOlderMessages.value
  ) {
    return;
  }

  const beforeId = getOldestLoadedMessageId();
  if (!beforeId) {
    return;
  }

  const container = messagesContainerRef.value;
  if (!container) {
    return;
  }

  loadingOlderMessages.value = true;
  const previousScrollHeight = container.scrollHeight;

  try {
    const response = await fetchMessagesService<IBaseResponse<IChatMessagePageResponse>>({
      uid: userLoginStore.id,
      session: activeSessionId.value,
      beforeId,
      pageSize: MESSAGE_PAGE_SIZE,
    });
    if (response.code !== 200 || !response.data) {
      ElMessage.error(response.message || t('chat.loadFailed'));
      return;
    }

    const pageData = parseMessagePageData(response.data);
    if (pageData.records.length === 0) {
      messagesHasMore.value = false;
      return;
    }

    suppressAutoScroll.value = true;
    messages.value = [...toChatMessageList(pageData.records), ...messages.value];
    messagesHasMore.value = pageData.hasMore;

    await nextTick();
    container.scrollTop = container.scrollHeight - previousScrollHeight;
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error));
  } finally {
    loadingOlderMessages.value = false;
    suppressAutoScroll.value = false;
  }
}

function handleMessagesScroll(event: Event) {
  const container = event.target as HTMLElement;
  updateStickToBottom(container);

  if (!canLoadOlderMessages.value || loadingMessages.value || loadingOlderMessages.value) {
    return;
  }

  if (!shouldLoadOlderMessages(container)) {
    return;
  }
  void loadOlderMessages();
}

/** 滚轮上滑时立即解除贴底，避免 rAF 打字跟滚把用户拽回去 */
function handleMessagesWheel(event: WheelEvent) {
  if (event.deltaY < 0) {
    stickToBottom.value = false;
  }
}

async function createSession(sessionId = uuidV4()): Promise<string | null> {
  if (!requireAuth()) {
    return null;
  }

  try {
    const response = await handleCreatSessionService<IBaseResponse<ICreateSessionResponse>>({
      uid: userLoginStore.id,
      session: sessionId,
    });
    if (response.code !== 200 || !response.data) {
      ElMessage.error(response.message || t('chat.createFailed'));
      return null;
    }

    const item = toSessionItem(response.data);
    activeSessionId.value = item.id;
    messages.value = [];
    messagesHasMore.value = false;
    canLoadOlderMessages.value = false;
    return item.id;
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error));
    return null;
  }
}

function handleSelectSession(sessionId: string) {
  if (sessionId === activeSessionId.value) {
    return;
  }
  activeSessionId.value = sessionId;
  void loadMessages(sessionId);
}

async function handleNewSession() {
  if (!requireAuth()) {
    return;
  }

  const sessionId = await createSession();
  if (!sessionId) {
    return;
  }
  await loadSessions(1);
  ElMessage.success(t('chat.createSuccess'));
}

function handleSearch(keyword: string) {
  searchKeyword.value = keyword;
  if (sessionSearchTimer) {
    clearTimeout(sessionSearchTimer);
  }
  sessionSearchTimer = setTimeout(() => {
    void loadSessions(1);
  }, SESSION_SEARCH_DEBOUNCE_MS);
}

async function handleDeleteSession(sessionId: string) {
  if (!requireAuth({ notify: false })) {
    return;
  }

  try {
    await ElMessageBox.confirm(t('chat.deleteSessionConfirm'), t('chat.deleteSessionTitle'), {
      type: 'warning',
      confirmButtonText: t('chat.deleteConfirm'),
      cancelButtonText: t('chat.cancel'),
      distinguishCancelAndClose: true,
    });
  } catch {
    return;
  }

  try {
    const response = await deleteSessionService<IBaseResponse<null>>({
      uid: userLoginStore.id,
      session: sessionId,
    });
    if (response.code !== 200) {
      ElMessage.error(response.message || t('chat.deleteSessionFailed'));
      return;
    }

    const wasActive = activeSessionId.value === sessionId;
    const nextPage =
      sessions.value.length === 1 && sessionPage.value > 1
        ? sessionPage.value - 1
        : sessionPage.value;

    if (wasActive) {
      activeSessionId.value = '';
      messages.value = [];
      messagesHasMore.value = false;
    }

    await loadSessions(nextPage);

    if (wasActive) {
      if (sessions.value.length > 0) {
        activeSessionId.value = sessions.value[0].id;
        await loadMessages(sessions.value[0].id);
      }
    }

    ElMessage.success(t('chat.deleteSessionSuccess'));
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error));
  }
}

async function handlePinSession(sessionId: string, pinned: boolean) {
  if (!requireAuth({ notify: false })) {
    return;
  }

  try {
    const response = await pinSessionService<IBaseResponse<null>>({
      uid: userLoginStore.id,
      session: sessionId,
      pinned,
    });
    if (response.code !== 200) {
      ElMessage.error(response.message || t('chat.pinSessionFailed'));
      return;
    }

    await loadSessions(sessionPage.value);
    ElMessage.success(pinned ? t('chat.pinSessionSuccess') : t('chat.unpinSessionSuccess'));
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error));
  }
}

function replaceTypingPlaceholder(message: ChatMessage) {
  const typingIndex = messages.value.findIndex((item) => item.typing)
  if (typingIndex >= 0) {
    messages.value[typingIndex] = { ...message, id: messages.value[typingIndex].id }
    return
  }
  messages.value.push(message)
}

function removeTypingPlaceholder() {
  messages.value = messages.value.filter((item) => !item.typing)
}

function buildAssistantMessage(data: IGeneralUnderstandingResponse): ChatMessage {
  return {
    id: `temp-assistant-${Date.now()}`,
    role: 'assistant',
    content: data.explanation,
    time: new Date().toLocaleString(),
    urgency: mapUrgencyLevel(data.urgency),
    sections: {
      advice: data.advice,
      notes: data.disclaimer,
    },
    animate: true,
  };
}

async function handleSend(text: string) {
  const trimmed = text.trim();
  if (!trimmed || sending.value) {
    return;
  }

  if (!(await ensureAuthWithPrompt())) {
    return;
  }

  let sessionId = activeSessionId.value;
  if (!sessionId) {
    sessionId = (await createSession()) ?? '';
  }
  if (!sessionId) {
    return;
  }

  const tempUserId = `temp-user-${Date.now()}`;
  const typingPlaceholderId = `pending-assistant-${Date.now()}`;
  const userMessage: ChatMessage = {
    id: tempUserId,
    role: 'user',
    content: trimmed,
    time: new Date().toLocaleString(),
  };

  pendingOutboundBySession.value = {
    ...pendingOutboundBySession.value,
    [sessionId]: { userMessage, typingPlaceholderId },
  };
  inflightSessionIds.value = new Set(inflightSessionIds.value).add(sessionId);

  messages.value.push(userMessage);
  messages.value.push({
    id: typingPlaceholderId,
    role: 'assistant',
    content: '',
    time: '',
    typing: true,
  });
  await scrollToBottom();

  try {
    const response = await handleGeneralUnderstandingService<
      IBaseResponse<IGeneralUnderstandingResponse>
    >({
      uid: userLoginStore.id,
      session: sessionId,
      message: trimmed,
    });

    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || t('chat.sendFailed'));
    }

    if (activeSessionId.value === sessionId) {
      replaceTypingPlaceholder(buildAssistantMessage(response.data));
      void loadSessions(1, true);
      await scrollToBottom();
    }
    clearPendingOutbound(sessionId);
  } catch (error) {
    if (activeSessionId.value === sessionId) {
      messages.value = messages.value.filter((message) => message.id !== tempUserId);
      removeTypingPlaceholder();
    }
    clearPendingOutbound(sessionId);
    ElMessage.error(getApiErrorMessage(error));
  } finally {
    const next = new Set(inflightSessionIds.value);
    next.delete(sessionId);
    inflightSessionIds.value = next;
  }
}

async function openLatestSession() {
  if (sessions.value.length === 0) {
    return;
  }
  const latestSessionId = sessions.value[0].id;
  activeSessionId.value = latestSessionId;
  await loadMessages(latestSessionId);
}

async function bootstrapChatPage() {
  if (!userLoginStore.id) {
    return;
  }

  const sessionFromQuery =
    typeof route.query.session === 'string' ? route.query.session.trim() : '';
  const initial = draftMessage.value.trim();

  if (initial) {
    await router.replace({ name: 'chat', query: {} });
    chatComposerRef.value?.resetDraft();
  }

  await loadSessions();

  if (initial) {
    const sessionId = await createSession();
    if (!sessionId) {
      return;
    }
    await handleSend(initial);
    chatComposerRef.value?.resetDraft();
    return;
  }

  if (sessionFromQuery) {
    await router.replace({ name: 'chat' });
    activeSessionId.value = sessionFromQuery;
    await loadMessages(sessionFromQuery);
    return;
  }

  if (!activeSessionId.value) {
    await openLatestSession();
  }
}

onMounted(() => {
  void bootstrapChatPage();
});
</script>

<template>
  <section class="chat-page">
    <LoginPromptDialog />
    <ChatSessionAside
      :sessions="sessions"
      :active-session-id="activeSessionId"
      :loading="loadingSessions"
      :page="sessionPage"
      :page-size="SESSION_PAGE_SIZE"
      :total="sessionTotal"
      @select="handleSelectSession"
      @new="handleNewSession"
      @search="handleSearch"
      @page-change="handleSessionPageChange"
      @delete="handleDeleteSession"
      @pin="handlePinSession"
    />

    <div class="chat-page__main">
      <header class="chat-page__header">
        <div class="chat-page__header-text">
          <h1 class="chat-page__title">{{ t('chat.with') }}</h1>
          <p v-if="currentTopic" class="chat-page__topic">
            {{ t('chat.current', { topic: currentTopic }) }}
          </p>
        </div>
      </header>

      <div
        ref="messagesContainerRef"
        class="chat-page__messages"
        :class="{ 'is-empty': showEmptyState }"
        @scroll="handleMessagesScroll"
        @wheel="handleMessagesWheel"
      >
        <div v-if="loadingMessages" class="chat-page__messages-loading">
          <MingmingLoadingTipContent :text="t('chat.loadingMessages')" />
        </div>
        <ChatEmptyState v-else-if="showEmptyState" />
        <template v-else>
          <ChatMessageHistoryLoader v-if="loadingOlderMessages" />
          <ChatMessageBubble
            v-for="message in messages"
            :key="message.id"
            :message="message"
            @typing-tick="followTypewriterScroll"
            @typing-complete="scrollToBottomIfNeeded"
          />
        </template>
      </div>

      <ChatComposer
        ref="chatComposerRef"
        :initial-value="draftMessage"
        :loading="sending"
        @send="handleSend"
      />
    </div>
  </section>
</template>

<style scoped lang="scss">
@use './styles/chat-view.scss';
</style>
