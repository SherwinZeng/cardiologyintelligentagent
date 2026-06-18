<script setup lang="ts">
import { ArrowDown, Bell, QuestionFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { storeToRefs } from 'pinia';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

import logoUrl from '@/assets/brand/logo.png';
import mingmingAvatarUrl from '@/assets/character/mingming-avatar.png';
import LocaleSwitch from '@/components/common/LocaleSwitch.vue';
import ThemeSwitch from '@/components/common/ThemeSwitch.vue';
import PrivacyShieldIcon from '@/components/icons/PrivacyShieldIcon.vue';
import { useUserLoginStore } from '@/stores/login';
import { buildLoginRoute } from '@/utils/resolveLoginRedirect';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const loginStore = useUserLoginStore();
const { displayName, userLoginStore, isLoggedIn } = storeToRefs(loginStore);

const avatarUrl = computed(() => userLoginStore.value.avatar || mingmingAvatarUrl);

function handleLogout() {
  loginStore.clearLogin();
  ElMessage.success(t('user.logout'));
  router.push('/');
}

function handleGoHelp() {
  void router.push({ name: 'help' });
}

function handleGoPrivacy() {
  void router.push({ name: 'privacy' });
}

function handleGoLogin() {
  void router.push(buildLoginRoute(route.fullPath));
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__brand">
      <span class="app-header__logo-wrap">
        <img class="app-header__logo" :src="logoUrl" :alt="t('app.name')" />
      </span>
      <span class="app-header__name">{{ t('app.name') }}</span>
    </div>

    <div class="app-header__actions">
      <button type="button" class="app-header__link" @click="handleGoPrivacy">
        <PrivacyShieldIcon class="app-header__link-icon app-header__link-icon--privacy" />
        <span class="app-header__link-text">{{ t('nav.privacy') }}</span>
      </button>
      <button type="button" class="app-header__link" @click="handleGoHelp">
        <el-icon class="app-header__link-icon"><QuestionFilled /></el-icon>
        <span class="app-header__link-text">{{ t('nav.help') }}</span>
      </button>
      <LocaleSwitch class="app-header__tool" />
      <ThemeSwitch class="app-header__tool" />
      <el-badge :value="3" class="app-header__badge">
        <el-button circle class="app-header__icon-btn" :aria-label="'notifications'">
          <el-icon><Bell /></el-icon>
        </el-button>
      </el-badge>

      <div class="app-header__user-menu">
        <button type="button" class="app-header__user" aria-haspopup="menu">
          <el-avatar :size="32" :src="avatarUrl" />
          <span class="app-header__username">{{ displayName || t('user.notLoggedIn') }}</span>
          <el-icon><ArrowDown /></el-icon>
        </button>

        <div class="app-header__user-dropdown" role="menu">
          <button
            v-if="isLoggedIn"
            type="button"
            class="app-header__user-dropdown-item is-danger"
            role="menuitem"
            @click="handleLogout"
          >
            {{ t('user.logout') }}
          </button>
          <button
            v-else
            type="button"
            class="app-header__user-dropdown-item"
            role="menuitem"
            @click="handleGoLogin"
          >
            {{ t('user.goLogin') }}
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped lang="scss">
@use './styles/app-header.scss';
</style>
