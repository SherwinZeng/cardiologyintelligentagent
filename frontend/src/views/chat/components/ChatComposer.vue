<script setup lang="ts">
import { Picture, Position } from '@element-plus/icons-vue'
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

import MingmingLoadingTipContent from '@/components/common/MingmingLoadingTipContent.vue'

/** 父组件可传入预填文案，例如首页跳转 /chat?message=胸口闷 */
const props = defineProps<{
  initialValue?: string
  loading?: boolean
}>()

/** 仅向父组件上报「用户要发送的内容」，本组件不调用接口 */
const emit = defineEmits<{
  send: [text: string]
}>()

const { t } = useI18n()

/** 输入框双向绑定的草稿内容 */
const draft = ref(props.initialValue ?? '')

/** 路由 query 变化时同步到输入框（如从首页带 message 进入） */
watch(
  () => props.initialValue,
  (value) => {
    if (value) {
      draft.value = value
    }
  },
)

/**
 * 发送入口：点击按钮或按 Enter 都会走到这里。
 * 只做校验 + 抛事件；清空输入、调 API、更新消息列表请在 ChatView / useChat 里处理。
 */
function handleSend() {
  const text = draft.value.trim()

  // 空内容不发送（按钮已 disabled，这里防止回车等边界情况）
  if (!text) {
    return
  }

  emit('send', text)
  draft.value = ''
}
</script>

<template>
  <footer class="chat-composer">
    <div class="chat-composer__inner">
      <el-tooltip placement="top" effect="light" popper-class="mingming-loading-tip">
        <template #content>
          <MingmingLoadingTipContent :text="t('chat.multimodalTip')" />
        </template>
        <span class="chat-composer__upload-wrap">
          <button type="button" class="chat-composer__upload" aria-label="upload" disabled>
            <el-icon><Picture /></el-icon>
          </button>
        </span>
      </el-tooltip>
      <input
        v-model="draft"
        class="chat-composer__input"
        type="text"
        :placeholder="t('chat.input')"
        @keydown.enter.prevent="handleSend"
      />
      <el-button
        type="primary"
        class="chat-composer__send"
        :loading="loading"
        :disabled="!draft.trim() || loading"
        @click="handleSend"
      >
        {{ t('chat.send') }}
        <el-icon><Position /></el-icon>
      </el-button>
    </div>
  </footer>
</template>

<style scoped lang="scss">
@use '../styles/chat-composer.scss';
</style>
