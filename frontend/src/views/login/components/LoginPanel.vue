<script setup lang="ts">
import {Iphone, Message} from '@element-plus/icons-vue'
import {reactive, ref} from 'vue'
import {useI18n} from 'vue-i18n'

import LocaleSwitch from '@/components/common/LocaleSwitch.vue'
import ThemeSwitch from '@/components/common/ThemeSwitch.vue'
import CuteHeartIcon from '@/components/icons/CuteHeartIcon.vue'
import AppFooter from '@/components/layout/AppFooter.vue'

import LoginMethodButton from './LoginMethodButton.vue'
import {useMultiLogin} from "@/hooks/useMultiLogin.ts";

const {t} = useI18n()

const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)

const form = reactive({
  phone: '',
  code: '',
  remember: false,
})

const {handleSmsLogin, handleGithubLogin, handleQqLogin, handleGuestLogin, sendSmsCode} = useMultiLogin()
</script>

<template>
  <section class="login-page__panel">
    <div class="login-page__panel-top">
      <LocaleSwitch/>
      <ThemeSwitch/>
    </div>

    <div class="login-page__card">
      <header class="login-page__header">
        <h2 class="login-page__title">{{ t('login.title') }}</h2>
        <p class="login-page__subtitle">{{ t('login.subtitle') }}</p>
      </header>

      <el-form class="login-page__form" @submit.prevent="handleSmsLogin">
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
                <Iphone/>
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
                  <Message/>
                </el-icon>
              </template>
            </el-input>
            <el-button
                class="login-page__send-code"
                size="large"
                :disabled="!form.phone.trim() || countdown > 0"
                :loading="sendingCode"
                @click="sendSmsCode"
            >
              {{
                countdown > 0
                    ? t('login.resendCode', {seconds: countdown})
                    : t('login.sendCode')
              }}
            </el-button>
          </div>
        </el-form-item>

        <div class="login-page__form-row login-page__form-row--single">
          <label class="login-page__remember">
            <input
                v-model="form.remember"
                class="login-page__remember-input"
                type="checkbox"
            />
            <span class="login-page__remember-heart" aria-hidden="true">
              <CuteHeartIcon :size="18"/>
            </span>
            <span class="login-page__remember-label">{{ t('login.remember') }}</span>
          </label>
        </div>

        <el-button
            class="login-page__submit"
            type="primary"
            size="large"
            native-type="submit"
            :loading="loading"
            :disabled="!form.phone.trim() || !form.code.trim()"
        >
          {{ t('login.submit') }}
        </el-button>

        <div class="login-page__divider">
          <span>{{ t('login.or') }}</span>
        </div>

        <div class="login-page__method-list">
          <LoginMethodButton
              provider="guest"
              :loading="loading"
              @click="handleGuestLogin"
          />
          <LoginMethodButton provider="qq" @click="handleQqLogin"/>
          <LoginMethodButton provider="github" @click="handleGithubLogin"/>
        </div>

        <p class="login-page__register">
          {{ t('login.noAccount') }}
          <button type="button" class="login-page__link">{{ t('login.registerNow') }}</button>
        </p>
      </el-form>

      <p class="login-page__disclaimer">{{ t('login.disclaimer') }}</p>
    </div>

    <AppFooter/>
  </section>
</template>

<style scoped lang="scss">
@use '../styles/login-panel.scss';
</style>
