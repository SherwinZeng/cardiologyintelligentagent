<script setup lang="ts">
import { computed } from 'vue';

export type RecordIconTheme = 'chest' | 'bp' | 'drug' | 'ecg' | 'report' | 'default';

const props = defineProps<{
  title?: string | null;
  compact?: boolean;
}>();

function resolveTheme(title?: string | null): RecordIconTheme {
  const text = title ?? '';
  if (/胸痛|胸闷|心前/.test(text)) return 'chest';
  if (/血压/.test(text)) return 'bp';
  if (/药|用药|降压|服药/.test(text)) return 'drug';
  if (/心电|波形|ECG|qrs|QRS|T波|U波/i.test(text)) return 'ecg';
  if (/报告|检查|体检|化验/.test(text)) return 'report';
  return 'default';
}

const theme = computed(() => resolveTheme(props.title));
</script>

<template>
  <div class="record-icon" :class="[`is-${theme}`, { 'is-compact': compact }]" aria-hidden="true">
    <!-- 胸痛 -->
    <svg v-if="theme === 'chest'" viewBox="0 0 48 48" fill="none">
      <path
        d="M24 8c-4.5 0-8 3.2-8 7.2 0 2.2.9 4.1 2.3 5.4L24 28l9.7-7.4c1.4-1.3 2.3-3.2 2.3-5.4C34 11.2 30.5 8 24 8Z"
        fill="currentColor"
        opacity="0.92"
      />
      <path
        d="M14 30c-2.8 2.4-4 5.2-4 8.2C10 42.8 16.2 44 24 44s16-1.2 16-5.8c0-3-1.2-5.8-4-8.2"
        stroke="currentColor"
        stroke-width="2.2"
        stroke-linecap="round"
      />
    </svg>

    <!-- 血压 -->
    <svg v-else-if="theme === 'bp'" viewBox="0 0 48 48" fill="none">
      <rect x="10" y="14" width="28" height="22" rx="4" stroke="currentColor" stroke-width="2.2" />
      <path d="M16 22h16M16 28h10" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" />
      <circle cx="30" cy="28" r="3" fill="currentColor" />
      <path d="M24 36v4" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" />
    </svg>

    <!-- 用药 -->
    <svg v-else-if="theme === 'drug'" viewBox="0 0 48 48" fill="none">
      <rect x="14" y="10" width="20" height="28" rx="6" stroke="currentColor" stroke-width="2.2" />
      <path d="M14 22h20" stroke="currentColor" stroke-width="2.2" />
      <rect x="20" y="28" width="8" height="6" rx="2" fill="currentColor" />
    </svg>

    <!-- 心电 -->
    <svg v-else-if="theme === 'ecg'" viewBox="0 0 48 48" fill="none">
      <path
        d="M8 28h7l4-10 5 20 5-14 4 8h9"
        stroke="currentColor"
        stroke-width="2.4"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <rect x="8" y="8" width="32" height="32" rx="6" stroke="currentColor" stroke-width="2" opacity="0.35" />
    </svg>

    <!-- 报告 -->
    <svg v-else-if="theme === 'report'" viewBox="0 0 48 48" fill="none">
      <path
        d="M16 8h14l6 6v26a2 2 0 0 1-2 2H16a2 2 0 0 1-2-2V10a2 2 0 0 1 2-2Z"
        stroke="currentColor"
        stroke-width="2.2"
      />
      <path d="M22 22h12M22 30h12M22 38h8" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" />
      <path d="M30 8v6h6" stroke="currentColor" stroke-width="2.2" stroke-linejoin="round" />
    </svg>

    <!-- 默认 -->
    <svg v-else viewBox="0 0 48 48" fill="none">
      <path
        d="M10 14a4 4 0 0 1 4-4h20a4 4 0 0 1 4 4v16a4 4 0 0 1-4 4H18l-8 6V14Z"
        stroke="currentColor"
        stroke-width="2.2"
        stroke-linejoin="round"
      />
      <path d="M18 20h12M18 28h8" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" />
    </svg>
  </div>
</template>

<style scoped lang="scss">
.record-icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 14px;
  color: #fff;

  svg {
    width: 34px;
    height: 34px;
  }

  &.is-chest {
    background: linear-gradient(145deg, #fb7185 0%, #ef4444 100%);
    box-shadow: 0 8px 18px rgb(239 68 68 / 28%);
  }

  &.is-bp {
    background: linear-gradient(145deg, #60a5fa 0%, #2563eb 100%);
    box-shadow: 0 8px 18px rgb(37 99 235 / 24%);
  }

  &.is-drug {
    background: linear-gradient(145deg, #fcd34d 0%, #f59e0b 100%);
    box-shadow: 0 8px 18px rgb(245 158 11 / 26%);
    color: #78350f;
  }

  &.is-ecg {
    background: linear-gradient(145deg, #6ee7b7 0%, #10b981 100%);
    box-shadow: 0 8px 18px rgb(16 185 129 / 24%);
  }

  &.is-report {
    background: linear-gradient(145deg, #86efac 0%, #22c55e 100%);
    box-shadow: 0 8px 18px rgb(34 197 94 / 22%);
  }

  &.is-default {
    background: linear-gradient(145deg, #93c5fd 0%, #3b82f6 100%);
    box-shadow: 0 8px 18px rgb(59 130 246 / 22%);
  }

  &.is-compact {
    display: grid;
    place-items: center;
    flex-shrink: 0;
    width: 40px;
    height: 40px;
    margin: 0;
    border-radius: 10px;
    box-shadow: none;

    svg {
      display: block;
      width: 22px;
      height: 22px;
      margin: 0;
    }
  }
}
</style>
