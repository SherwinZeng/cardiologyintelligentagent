<script setup lang="ts">
import { User } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

import mingmingWelcomeUrl from '@/assets/character/mingming-welcome-q.png'
import { useLoginPrompt } from '@/hooks/useLoginPrompt.ts'
import { useMultiLogin } from '@/hooks/useMultiLogin.ts'

const { t } = useI18n()
const { visible, resolveLoginPrompt } = useLoginPrompt()
const { handleGuestLogin } = useMultiLogin()

const guestLoading = ref(false)

function handleGoLogin() {
  resolveLoginPrompt('login')
}

async function handleGuestTrial() {
  if (guestLoading.value) {
    return
  }

  guestLoading.value = true
  try {
    const success = await handleGuestLogin({ navigate: false })
    if (success) {
      resolveLoginPrompt('guest')
    }
  } finally {
    guestLoading.value = false
  }
}

function handleClose() {
  resolveLoginPrompt('cancel')
}
</script>

<template>
  <el-dialog
    v-model="visible"
    class="login-prompt-dialog"
    :title="t('auth.loginPromptTitle')"
    width="400px"
    align-center
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="login-prompt-dialog__body">
      <img
        class="login-prompt-dialog__avatar"
        :src="mingmingWelcomeUrl"
        :alt="t('ai.assistant')"
      />
      <p class="login-prompt-dialog__message">{{ t('auth.loginPromptMessage') }}</p>
    </div>

    <template #footer>
      <div class="login-prompt-dialog__actions">
        <el-button type="primary" size="large" @click="handleGoLogin">
          {{ t('auth.goLogin') }}
        </el-button>

        <el-tooltip
          placement="top"
          effect="light"
          :content="t('auth.guestTrialTip')"
        >
          <span class="login-prompt-dialog__guest-wrap">
            <el-button
              size="large"
              class="login-prompt-dialog__guest-btn"
              :loading="guestLoading"
              @click="handleGuestTrial"
            >
              <el-icon class="login-prompt-dialog__guest-icon"><User /></el-icon>
              {{ t('auth.guestTrial') }}
            </el-button>
          </span>
        </el-tooltip>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
@use './styles/login-prompt-dialog.scss';
</style>
