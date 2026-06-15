import { useColorMode, useLocalStorage } from '@vueuse/core'
import { defineStore } from 'pinia'
import { computed } from 'vue'

export type ThemePreference = 'light' | 'dark' | 'system'

export const useThemeStore = defineStore('theme', () => {
  const preference = useLocalStorage<ThemePreference>('cardiology-theme', 'system')

  const colorMode = useColorMode({
    attribute: 'class',
    modes: {
      light: '',
      dark: 'dark',
    },
    storageKey: null,
  })

  const resolvedTheme = computed<'light' | 'dark'>(() =>
    colorMode.value === 'dark' ? 'dark' : 'light',
  )

  function syncColorMode() {
    colorMode.value = preference.value === 'system' ? 'auto' : preference.value
  }

  function applyPreference(next: ThemePreference) {
    preference.value = next
    syncColorMode()
  }

  function init() {
    syncColorMode()
  }

  return {
    preference,
    resolvedTheme,
    applyPreference,
    init,
  }
})
