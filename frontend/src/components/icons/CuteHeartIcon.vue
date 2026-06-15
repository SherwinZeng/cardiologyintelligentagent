<script setup lang="ts">
import { useId } from 'vue'

withDefaults(
  defineProps<{
    size?: number | string
  }>(),
  {
    size: 48,
  },
)

const uid = useId().replace(/:/g, '')
const bodyGradientId = `cute-heart-body-${uid}`
const shineGradientId = `cute-heart-shine-${uid}`
const shadowFilterId = `cute-heart-shadow-${uid}`
</script>

<template>
  <svg
    class="cute-heart-icon"
    :width="size"
    :height="size"
    viewBox="0 0 64 64"
    fill="none"
    aria-hidden="true"
  >
    <defs>
      <linearGradient :id="bodyGradientId" x1="18" y1="10" x2="48" y2="58" gradientUnits="userSpaceOnUse">
        <stop offset="0%" stop-color="#ffe4ef" />
        <stop offset="38%" stop-color="#fb9fbd" />
        <stop offset="100%" stop-color="#f43f5e" />
      </linearGradient>
      <radialGradient :id="shineGradientId" cx="24" cy="20" r="18" gradientUnits="userSpaceOnUse">
        <stop offset="0%" stop-color="#ffffff" stop-opacity="0.95" />
        <stop offset="100%" stop-color="#ffffff" stop-opacity="0" />
      </radialGradient>
      <filter :id="shadowFilterId" x="-20%" y="-20%" width="140%" height="140%">
        <feDropShadow dx="0" dy="3" stdDeviation="3" flood-color="#fb7185" flood-opacity="0.35" />
      </filter>
    </defs>

    <path
      :fill="`url(#${bodyGradientId})`"
      :filter="`url(#${shadowFilterId})`"
      d="M32 54.5c-1.2 0-2.4-.5-3.3-1.4C19.8 44.6 11 36.8 11 25.5 11 17.4 17.4 11 25.5 11c4.6 0 8.9 2.2 11.6 5.8 2.7-3.6 7-5.8 11.6-5.8 8.1 0 14.5 6.4 14.5 14.5 0 11.3-8.8 19.1-17.7 27.6-.9.9-2.1 1.4-3.3 1.4Z"
    />
    <ellipse cx="24" cy="22" rx="9" ry="7" :fill="`url(#${shineGradientId})`" />
    <path
      fill="#ffffff"
      fill-opacity="0.35"
      d="M25.5 18.5c2.8-2.2 6.8-1.8 9.1 1 .8.9 1.3 2 1.5 3.1-2.4-1.6-5.6-1.2-7.6 1.1-1.2 1.4-1.8 3.1-1.9 4.8-.6-2.8.2-5.8 2.4-7.9-.4.3-.9.5-1.5.9Z"
    />
  </svg>
</template>

<style scoped lang="scss">
.cute-heart-icon {
  display: block;
}
</style>
