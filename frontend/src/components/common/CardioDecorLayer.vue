<script setup lang="ts">
import type { CardioDecorItem } from './cardioDecor'
import { CHAT_EMPTY_DECOR_ITEMS } from './cardioDecor'

withDefaults(
  defineProps<{
    items?: CardioDecorItem[]
    maskCenter?: string
  }>(),
  {
    items: () => CHAT_EMPTY_DECOR_ITEMS,
    maskCenter: '50% 50%',
  },
)
</script>

<template>
  <div
    class="cardio-decor"
    :style="{ '--cardio-decor-mask-center': maskCenter }"
    aria-hidden="true"
  >
    <svg
      v-for="item in items"
      :key="item.id"
      class="cardio-decor__item"
      :class="`is-motion-${item.motion}`"
      :style="{
        top: item.top,
        left: item.left,
        width: `${item.size}px`,
        height: `${item.size}px`,
        animationDelay: `${item.delay}s`,
      }"
      viewBox="0 0 24 24"
    >
      <template v-if="item.type === 'heart'">
        <path
          fill="currentColor"
          d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
        />
      </template>
      <template v-else-if="item.type === 'stethoscope'">
        <path
          fill="none"
          stroke="currentColor"
          stroke-width="1.4"
          stroke-linecap="round"
          d="M9 3a2.5 2.5 0 0 1 5 0v4.5a3.5 3.5 0 0 1-7 0V3m2.5 7.5V14m-2.5 2.5h5"
        />
        <circle cx="17.5" cy="17.5" r="2" fill="none" stroke="currentColor" stroke-width="1.4" />
        <path fill="none" stroke="currentColor" stroke-width="1.4" d="M14 14.5c0-1.9 1.6-3.5 3.5-3.5" />
      </template>
      <template v-else-if="item.type === 'pulse'">
        <path
          fill="none"
          stroke="currentColor"
          stroke-width="1.4"
          stroke-linecap="round"
          stroke-linejoin="round"
          d="M2 12h3l2-4 2.5 8 2-4h8"
        />
      </template>
      <template v-else-if="item.type === 'bp-cuff'">
        <rect
          x="4"
          y="8"
          width="12"
          height="8"
          rx="2"
          fill="none"
          stroke="currentColor"
          stroke-width="1.4"
        />
        <path fill="none" stroke="currentColor" stroke-width="1.4" d="M16 10h2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2h-2" />
        <path fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" d="M8 16v2" />
      </template>
      <template v-else-if="item.type === 'pill'">
        <rect
          x="5"
          y="9"
          width="14"
          height="6"
          rx="3"
          fill="none"
          stroke="currentColor"
          stroke-width="1.4"
        />
        <path fill="none" stroke="currentColor" stroke-width="1.4" d="M12 9v6" />
      </template>
      <template v-else>
        <path
          fill="none"
          stroke="currentColor"
          stroke-width="1.4"
          stroke-linecap="round"
          d="M12 5v14M5 12h14"
        />
      </template>
    </svg>
  </div>
</template>

<style scoped lang="scss">
@use './styles/cardio-decor-layer.scss';
</style>
