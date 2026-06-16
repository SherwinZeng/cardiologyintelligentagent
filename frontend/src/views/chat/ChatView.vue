<script setup lang="ts">
import {v4 as uuidV4} from 'uuid'
import {useI18n} from 'vue-i18n'

import ChatComposer from './components/ChatComposer.vue'
import ChatMessageBubble from './components/ChatMessageBubble.vue'
import ChatSessionAside from './components/ChatSessionAside.vue'
import type {ChatMessage, ChatSessionItem} from './types'
import {useAsyncAction} from "@/hooks/useAsyncAction";
import {handleCreatSessionService} from '@/services/chat/handleSession.ts'
import {useUserLoginStore} from "@/stores/login.ts";
import {ElMessage} from "element-plus";
import {getApiErrorMessage} from '@/http'
import type {IBaseResponse} from '@/typings/baseResponse.ts'
import type {ICreateSessionResponse} from '@/typings/chat/session.ts'

const {t} = useI18n()
const route = useRoute()
const {run} = useAsyncAction()
const {userLoginStore} = useUserLoginStore()

const sessions = ref<ChatSessionItem[]>([])

const messages = ref<ChatMessage[]>([])

const activeSessionId = ref('')

const draftMessage = computed(() => {
  const value = route.query.message
  return typeof value === 'string' ? value : ''
})

const currentTopic = computed(
    () => sessions.value.find((item) => item.id === activeSessionId.value)?.title ?? '',
)

function handleSelectSession(sessionId: string) {
  activeSessionId.value = sessionId
}

function handleNewSession() {
  const sessionId = uuidV4()
  run<IBaseResponse<ICreateSessionResponse>>(
      () =>
          handleCreatSessionService({
            uid: userLoginStore.id,
            session: sessionId,
          }),
      (response) => {
        if (response.code !== 200 || !response.data) {
          ElMessage.error(response.message || '创建失败')
          return
        }
        const item = response.data as unknown as ChatSessionItem
        sessions.value.unshift(item)
        activeSessionId.value = item.id
        messages.value = []
        ElMessage.success("创建成功")
      },
      (error) => {
        ElMessage.error(getApiErrorMessage(error))
      },
  )
}

function handleSearch(keyword: string) {
  void keyword
}

function handleSend(text: string) {
  void text
}
</script>

<template>
  <section class="chat-page">
    <ChatSessionAside
        :sessions="sessions"
        :active-session-id="activeSessionId"
        @select="handleSelectSession"
        @new="handleNewSession"
        @search="handleSearch"
    />

    <div class="chat-page__main">
      <header class="chat-page__header">
        <div class="chat-page__header-text">
          <h1 class="chat-page__title">{{ t('chat.with') }}</h1>
          <p v-if="currentTopic" class="chat-page__topic">
            {{ t('chat.current', {topic: currentTopic}) }}
          </p>
        </div>
      </header>

      <div class="chat-page__messages">
        <ChatMessageBubble
            v-for="message in messages"
            :key="message.id"
            :message="message"
        />
        <p v-if="messages.length === 0" class="chat-page__empty">
          {{ t('records.empty') }}
        </p>
      </div>

      <ChatComposer :initial-value="draftMessage" @send="handleSend"/>
    </div>
  </section>
</template>

<style scoped lang="scss">
@use './styles/chat-view.scss';
</style>
