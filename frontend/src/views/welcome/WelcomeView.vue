<script setup lang="ts">
import { ArrowRight, Picture, TrendCharts } from '@element-plus/icons-vue'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

import mingmingWelcomeUrl from '@/assets/character/mingming-welcome-q.png'
import MingmingLoadingTipContent from '@/components/common/MingmingLoadingTipContent.vue'
import CuteHeartIcon from '@/components/icons/CuteHeartIcon.vue'
import PrivacyShieldIcon from '@/components/icons/PrivacyShieldIcon.vue'

import WelcomeEcgWave from './components/WelcomeEcgWave.vue'

const { t, tm } = useI18n()
const router = useRouter()

const message = ref('')

const dailyTip = computed(() => {
  const tips = tm('welcome.tips')
  const count = Array.isArray(tips) ? tips.length : 0
  if (count === 0) {
    return ''
  }

  const dayIndex = Math.floor(Date.now() / 86_400_000) % count
  return t(`welcome.tips[${dayIndex}]`)
})

const chips = computed(() => [
  {
    key: 'welcome.chip1',
    text: t('welcome.chip1'),
    icon: 'heart',
    tone: 'red',
  },
  {
    key: 'welcome.chip2',
    text: t('welcome.chip2'),
    icon: 'bp',
    tone: 'blue',
  },
  {
    key: 'welcome.chip3',
    text: t('welcome.chip3'),
    icon: TrendCharts,
    tone: 'green',
  },
])

function applyChip(text: string) {
  message.value = text
}

function startConsultation() {
  const trimmed = message.value.trim()
  if (!trimmed) {
    return
  }

  router.push({
    name: 'chat',
    query: { message: trimmed },
  })
}

type DecorMotion = 'float' | 'heartbeat' | 'drift' | 'spin' | 'glow'
type DecorType = 'heart' | 'stethoscope' | 'pulse' | 'bp-cuff' | 'pill' | 'cross'

interface DecorItem {
  id: string
  type: DecorType
  top: string
  left: string
  size: number
  motion: DecorMotion
  delay: number
}

const decorItems: DecorItem[] = [
  { id: 'd01', type: 'heart', top: '6%', left: '5%', size: 26, motion: 'heartbeat', delay: 0 },
  { id: 'd02', type: 'pulse', top: '7%', left: '18%', size: 30, motion: 'drift', delay: 0.6 },
  { id: 'd03', type: 'stethoscope', top: '9%', left: '32%', size: 28, motion: 'float', delay: 1.1 },
  { id: 'd04', type: 'pill', top: '6%', left: '68%', size: 24, motion: 'spin', delay: 0.3 },
  { id: 'd05', type: 'pulse', top: '8%', left: '82%', size: 32, motion: 'drift', delay: 1.8 },
  { id: 'd06', type: 'heart', top: '10%', left: '93%', size: 22, motion: 'heartbeat', delay: 2.2 },
  { id: 'd07', type: 'cross', top: '22%', left: '3%', size: 20, motion: 'glow', delay: 0.9 },
  { id: 'd08', type: 'pulse', top: '26%', left: '12%', size: 28, motion: 'drift', delay: 2.6 },
  { id: 'd09', type: 'bp-cuff', top: '24%', left: '88%', size: 28, motion: 'float', delay: 1.4 },
  { id: 'd10', type: 'heart', top: '28%', left: '95%', size: 24, motion: 'heartbeat', delay: 0.4 },
  { id: 'd11', type: 'stethoscope', top: '48%', left: '2%', size: 26, motion: 'float', delay: 2.0 },
  { id: 'd12', type: 'pill', top: '52%', left: '94%', size: 22, motion: 'spin', delay: 1.6 },
  { id: 'd13', type: 'pulse', top: '68%', left: '6%', size: 30, motion: 'drift', delay: 0.2 },
  { id: 'd14', type: 'heart', top: '72%', left: '16%', size: 22, motion: 'heartbeat', delay: 2.8 },
  { id: 'd15', type: 'cross', top: '70%', left: '84%', size: 20, motion: 'glow', delay: 1.0 },
  { id: 'd16', type: 'bp-cuff', top: '74%', left: '92%', size: 26, motion: 'float', delay: 2.4 },
  { id: 'd17', type: 'pulse', top: '84%', left: '10%', size: 28, motion: 'drift', delay: 1.2 },
  { id: 'd18', type: 'stethoscope', top: '86%', left: '24%', size: 24, motion: 'float', delay: 0.7 },
  { id: 'd19', type: 'heart', top: '85%', left: '76%', size: 24, motion: 'heartbeat', delay: 1.9 },
  { id: 'd20', type: 'pill', top: '88%', left: '88%', size: 22, motion: 'spin', delay: 2.1 },
]
</script>

<template>
  <section class="welcome-page">
    <div class="welcome-page__stage">
      <p v-if="dailyTip" class="welcome-page__tip">
        <span class="welcome-page__tip-label">{{ t('welcome.tipLabel') }}</span>
        <span class="welcome-page__tip-text">{{ dailyTip }}</span>
      </p>

      <div class="welcome-page__backdrop" aria-hidden="true">
        <div class="welcome-page__heart" />
        <WelcomeEcgWave />
        <div class="welcome-page__decor">
          <svg
            v-for="item in decorItems"
            :key="item.id"
            class="welcome-page__decor-item"
            :class="`is-motion-${item.motion}`"
            :style="{
              top: item.top,
              left: item.left,
              width: `${item.size}px`,
              height: `${item.size}px`,
              animationDelay: `${item.delay}s`,
            }"
            viewBox="0 0 24 24"
            aria-hidden="true"
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
      </div>

      <div class="welcome-page__hero">
        <div class="welcome-page__character-wrap">
          <div class="welcome-page__character-shell">
            <div class="welcome-page__character-aura" aria-hidden="true" />
            <img
              class="welcome-page__character"
              :src="mingmingWelcomeUrl"
              :alt="t('welcome.hi')"
            />
            <span class="welcome-page__palm-heart" aria-hidden="true">
              <CuteHeartIcon size="100%" />
            </span>
          </div>
        </div>

        <h1 class="welcome-page__title">{{ t('welcome.hi') }}</h1>
        <p class="welcome-page__subtitle">{{ t('app.tagline') }}</p>

        <div class="welcome-page__chips">
          <button
            v-for="chip in chips"
            :key="chip.key"
            type="button"
            class="welcome-page__chip"
            @click="applyChip(chip.text)"
          >
            <span class="welcome-page__chip-icon" :class="`is-${chip.tone}`">
              <svg
                v-if="chip.icon === 'heart'"
                viewBox="0 0 24 24"
                width="14"
                height="14"
                aria-hidden="true"
              >
                <path
                  fill="currentColor"
                  d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                />
              </svg>
              <svg
                v-else-if="chip.icon === 'bp'"
                viewBox="0 0 24 24"
                width="14"
                height="14"
                aria-hidden="true"
              >
                <path
                  fill="currentColor"
                  d="M12 2C9.24 2 7 4.24 7 7v1.07C4.84 8.56 3 10.78 3 13.5 3 17.09 5.91 20 9.5 20h5c3.59 0 6.5-2.91 6.5-6.5 0-2.72-1.84-4.94-4.5-6.43V7c0-2.76-2.24-5-5-5zm0 2c1.66 0 3 1.34 3 3v1.08l.6.24C16.37 9.18 18 11.18 18 13.5 18 15.99 15.99 18 13.5 18h-5C6.01 18 4 15.99 4 13.5c0-2.32 1.63-4.32 3.9-5.18l.6-.24V7c0-1.66 1.34-3 3-3z"
                />
              </svg>
              <el-icon v-else><component :is="chip.icon" /></el-icon>
            </span>
            <span>{{ chip.text }}</span>
          </button>
        </div>

        <div class="welcome-page__composer">
          <div class="welcome-page__input-wrap">
            <input
              v-model="message"
              class="welcome-page__input"
              type="text"
              :placeholder="t('welcome.input')"
              @keydown.enter.prevent="startConsultation"
            />
            <el-tooltip placement="top" effect="light" popper-class="mingming-loading-tip">
              <template #content>
                <MingmingLoadingTipContent :text="t('chat.multimodalTip')" />
              </template>
              <span class="welcome-page__upload-wrap">
                <button type="button" class="welcome-page__upload" aria-label="upload" disabled>
                  <el-icon><Picture /></el-icon>
                </button>
              </span>
            </el-tooltip>
          </div>
          <el-button
            type="primary"
            class="welcome-page__start"
            :disabled="!message.trim()"
            @click="startConsultation"
          >
            {{ t('welcome.start') }}
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <p class="welcome-page__disclaimer">
      <PrivacyShieldIcon class="welcome-page__disclaimer-icon" />
      {{ t('welcome.footer') }}
    </p>
  </section>
</template>

<style scoped lang="scss">
@use './styles/welcome-view.scss';
</style>
