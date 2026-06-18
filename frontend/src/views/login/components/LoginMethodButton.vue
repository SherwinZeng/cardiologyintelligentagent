<script setup lang="ts">
import { User } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';

import githubIconUrl from '@/assets/icons/brand/github.svg?url';
import qqIconUrl from '@/assets/icons/brand/qq.svg?url';
import MingmingLoadingTipContent from '@/components/common/MingmingLoadingTipContent.vue';

defineProps<{
  provider: 'guest' | 'qq' | 'github';
  loading?: boolean;
  disabled?: boolean;
}>();

defineEmits<{
  click: [];
}>();

const { t } = useI18n();
</script>

<template>
  <el-tooltip v-if="disabled" placement="bottom" effect="light" popper-class="mingming-loading-tip">
    <template #content>
      <MingmingLoadingTipContent />
    </template>
    <div class="login-page__tooltip-wrap">
      <button
        type="button"
        class="login-page__method-btn is-under-construction"
        :class="`login-page__method-btn--${provider}`"
        disabled
      >
        <el-icon v-if="provider === 'guest'" class="login-page__method-icon">
          <User />
        </el-icon>
        <img
          v-else-if="provider === 'qq'"
          class="login-page__method-icon login-page__method-icon--brand"
          :src="qqIconUrl"
          alt=""
          aria-hidden="true"
        />
        <img
          v-else
          class="login-page__method-icon login-page__method-icon--brand"
          :src="githubIconUrl"
          alt=""
          aria-hidden="true"
        />
        <span>{{ t(`login.${provider}`) }}</span>
      </button>
    </div>
  </el-tooltip>

  <button
    v-else
    type="button"
    class="login-page__method-btn"
    :class="`login-page__method-btn--${provider}`"
    :disabled="loading"
    @click="$emit('click')"
  >
    <el-icon v-if="provider === 'guest'" class="login-page__method-icon">
      <User />
    </el-icon>
    <img
      v-else-if="provider === 'qq'"
      class="login-page__method-icon login-page__method-icon--brand"
      :src="qqIconUrl"
      alt=""
      aria-hidden="true"
    />
    <img
      v-else
      class="login-page__method-icon login-page__method-icon--brand"
      :src="githubIconUrl"
      alt=""
      aria-hidden="true"
    />
    <span>{{ t(`login.${provider}`) }}</span>
  </button>
</template>

<style scoped lang="scss">
@use '../styles/login-method-button.scss';

.login-page__tooltip-wrap,
.login-page__tooltip-wrap.el-tooltip__trigger {
  display: block;
  width: 100%;
}
</style>
