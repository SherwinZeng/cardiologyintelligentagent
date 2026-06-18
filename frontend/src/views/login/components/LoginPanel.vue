<script setup lang="ts">
import { Iphone, Message } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onUnmounted, reactive, ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';

import LocaleSwitch from '@/components/common/LocaleSwitch.vue';
import MingmingLoadingTipContent from '@/components/common/MingmingLoadingTipContent.vue';
import ThemeSwitch from '@/components/common/ThemeSwitch.vue';
import AppFooter from '@/components/layout/AppFooter.vue';
import { useMultiLogin } from '@/hooks/useMultiLogin.ts';
import { fetchCaptcha } from '@/services/login/fetchCaptcha.ts';

import LoginMethodButton from './LoginMethodButton.vue';

const { t } = useI18n();

const loading = ref(false);
const sendingCode = ref(false);
const loadingCaptcha = ref(false);
const countdown = ref(0);
const captchaModalVisible = ref(false);
const captchaId = ref('');
const captchaImage = ref('');
const modalCaptchaCode = ref('');
let countdownTimer: number | undefined;

const form = reactive({
  phone: '',
  code: '',
});

const { handleSmsLogin, handleGuestLogin, sendSmsCode } = useMultiLogin();

const canSendCode = computed(() => Boolean(form.phone.trim()) && countdown.value <= 0);
const canSubmit = computed(() => Boolean(form.phone.trim()) && Boolean(form.code.trim()));

const sendCodeTip = computed(() => {
  if (!form.phone.trim()) {
    return t('login.phoneRequired');
  }
  if (countdown.value > 0) {
    return t('login.sendCodeTipWait', { seconds: countdown.value });
  }
  return '';
});

const submitTip = computed(() => {
  const hasPhone = Boolean(form.phone.trim());
  const hasCode = Boolean(form.code.trim());
  if (!hasPhone && !hasCode) {
    return t('login.submitTipBoth');
  }
  if (!hasPhone) {
    return t('login.phoneRequired');
  }
  if (!hasCode) {
    return t('login.codeRequired');
  }
  return '';
});

function onSmsLoginSubmit() {
  if (!form.phone.trim() || !form.code.trim()) {
    return;
  }
  handleSmsLogin({
    phone: form.phone.trim(),
    code: form.code.trim(),
  });
}

async function loadCaptcha() {
  const phone = form.phone.trim();
  if (!phone) {
    return;
  }
  loadingCaptcha.value = true;
  try {
    const captcha = await fetchCaptcha(phone);
    captchaId.value = captcha.captchaId;
    captchaImage.value = captcha.captchaImage;
    modalCaptchaCode.value = '';
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('login.captchaFailed'));
  } finally {
    loadingCaptcha.value = false;
  }
}

function startCountdown() {
  countdown.value = 60;
  if (countdownTimer) {
    window.clearInterval(countdownTimer);
  }
  countdownTimer = window.setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0 && countdownTimer) {
      window.clearInterval(countdownTimer);
      countdownTimer = undefined;
    }
  }, 1000);
}

function openCaptchaModal() {
  if (!form.phone.trim()) {
    ElMessage.warning(t('login.phoneRequired'));
    return;
  }
  if (countdown.value > 0) {
    return;
  }
  captchaModalVisible.value = true;
  loadCaptcha();
}

function closeCaptchaModal() {
  captchaModalVisible.value = false;
  modalCaptchaCode.value = '';
}

async function confirmCaptchaAndSend() {
  if (!modalCaptchaCode.value.trim()) {
    ElMessage.warning(t('login.captchaRequired'));
    return;
  }
  if (!captchaId.value) {
    ElMessage.warning(t('login.captchaFailed'));
    return;
  }
  sendingCode.value = true;
  try {
    const success = await sendSmsCode({
      phone: form.phone.trim(),
      captchaId: captchaId.value,
      captchaCode: modalCaptchaCode.value.trim(),
    });
    if (success) {
      closeCaptchaModal();
      captchaId.value = '';
      captchaImage.value = '';
      startCountdown();
      return;
    }
    await loadCaptcha();
  } finally {
    sendingCode.value = false;
  }
}

onUnmounted(() => {
  if (countdownTimer) {
    window.clearInterval(countdownTimer);
  }
});
</script>

<template>
  <section class="login-page__panel">
    <div class="login-page__panel-top">
      <LocaleSwitch />
      <ThemeSwitch />
    </div>

    <div class="login-page__card">
      <header class="login-page__header">
        <h2 class="login-page__title">{{ t('login.title') }}</h2>
        <p class="login-page__subtitle">{{ t('login.subtitle') }}</p>
      </header>

      <el-form class="login-page__form" @submit.prevent="onSmsLoginSubmit">
        <el-form-item>
          <el-input
            v-model="form.phone"
            size="large"
            :placeholder="t('login.phone')"
            autocomplete="tel"
            maxlength="11"
          >
            <template #prefix>
              <el-icon>
                <Iphone />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item class="login-page__code-item">
          <div class="login-page__code-row">
            <el-input
              v-model="form.code"
              size="large"
              :placeholder="t('login.code')"
              autocomplete="one-time-code"
              maxlength="6"
            >
              <template #prefix>
                <el-icon>
                  <Message />
                </el-icon>
              </template>
            </el-input>
            <el-tooltip
              placement="bottom"
              effect="light"
              popper-class="mingming-loading-tip"
              :disabled="canSendCode"
            >
              <template #content>
                <MingmingLoadingTipContent :text="sendCodeTip" />
              </template>
              <span class="login-page__send-code-wrap">
                <el-button
                  class="login-page__send-code"
                  size="large"
                  :disabled="!canSendCode"
                  @click="openCaptchaModal"
                >
                  {{
                    countdown > 0
                      ? t('login.resendCode', { seconds: countdown })
                      : t('login.sendCode')
                  }}
                </el-button>
              </span>
            </el-tooltip>
          </div>
        </el-form-item>

        <el-tooltip
          placement="bottom"
          effect="light"
          popper-class="mingming-loading-tip"
          :disabled="canSubmit"
        >
          <template #content>
            <MingmingLoadingTipContent :text="submitTip" />
          </template>
          <div class="login-page__submit-wrap">
            <el-button
              class="login-page__submit"
              type="primary"
              size="large"
              native-type="submit"
              :loading="loading"
              :disabled="!canSubmit"
            >
              {{ t('login.submit') }}
            </el-button>
          </div>
        </el-tooltip>

        <div class="login-page__divider">
          <span>{{ t('login.or') }}</span>
        </div>

        <div class="login-page__method-list">
          <LoginMethodButton provider="guest" :loading="loading" @click="handleGuestLogin" />
          <LoginMethodButton provider="qq" disabled />
          <LoginMethodButton provider="github" disabled />
        </div>
      </el-form>

      <p class="login-page__disclaimer">{{ t('login.disclaimer') }}</p>
    </div>

    <el-dialog
      v-model="captchaModalVisible"
      class="login-page__captcha-dialog"
      :title="t('login.captchaModalTitle')"
      width="360px"
      align-center
      :close-on-click-modal="false"
      @closed="closeCaptchaModal"
    >
      <p class="login-page__captcha-tip">{{ t('login.captchaModalTip') }}</p>
      <button
        type="button"
        class="login-page__captcha-modal-img-btn"
        :disabled="loadingCaptcha"
        :title="t('login.captchaRefresh')"
        @click="loadCaptcha"
      >
        <img
          v-if="captchaImage"
          :src="captchaImage"
          class="login-page__captcha-modal-img"
          alt="captcha"
        />
        <span v-else class="login-page__captcha-placeholder">
          {{ loadingCaptcha ? t('login.captchaLoading') : t('login.captchaFetch') }}
        </span>
      </button>
      <el-input
        v-model="modalCaptchaCode"
        size="large"
        class="login-page__captcha-modal-input"
        :placeholder="t('login.captcha')"
        autocomplete="off"
        maxlength="6"
        @keyup.enter="confirmCaptchaAndSend"
      />
      <template #footer>
        <el-button @click="closeCaptchaModal">{{ t('login.captchaCancel') }}</el-button>
        <el-button
          type="primary"
          :loading="sendingCode"
          :disabled="loadingCaptcha"
          @click="confirmCaptchaAndSend"
        >
          {{ t('login.captchaConfirm') }}
        </el-button>
      </template>
    </el-dialog>

    <AppFooter />
  </section>
</template>

<style scoped lang="scss">
@use '../styles/login-panel.scss';
</style>

<style lang="scss">
.login-page__send-code-wrap,
.login-page__send-code-wrap.el-tooltip__trigger {
  display: inline-flex;
  flex-shrink: 0;
}

.login-page__submit-wrap,
.login-page__submit-wrap.el-tooltip__trigger {
  display: block;
  width: 100%;
}
</style>
