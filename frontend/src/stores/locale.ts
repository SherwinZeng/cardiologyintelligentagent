import { useLocalStorage } from '@vueuse/core';
import { defineStore } from 'pinia';

import i18n, {
  LOCALE_STORAGE_KEY,
  normalizeLocale,
  readStoredLocale,
  type AppLocale,
} from '@/i18n';

export const useLocaleStore = defineStore('locale', () => {
  const locale = useLocalStorage<AppLocale>(LOCALE_STORAGE_KEY, readStoredLocale());

  function applyLocale(next: AppLocale) {
    const normalized = normalizeLocale(next);
    locale.value = normalized;
    i18n.global.locale.value = normalized;
    document.documentElement.lang = normalized;
  }

  function init() {
    applyLocale(locale.value);
  }

  return {
    locale,
    applyLocale,
    init,
  };
});
