import { createI18n } from 'vue-i18n';

import enUS from '@/locales/en-US.json';
import zhCN from '@/locales/zh-CN.json';

export type AppLocale = 'zh-CN' | 'en-US';

export const SUPPORT_LOCALES: AppLocale[] = ['zh-CN', 'en-US'];
export const LOCALE_STORAGE_KEY = 'cardiology-locale';

export function normalizeLocale(value: unknown): AppLocale {
  if (value === 'zh-CN') {
    return 'zh-CN';
  }

  if (value === 'en-US' || value === 'en') {
    return 'en-US';
  }

  return 'zh-CN';
}

export function readStoredLocale(): AppLocale {
  try {
    return normalizeLocale(localStorage.getItem(LOCALE_STORAGE_KEY));
  } catch {
    return 'zh-CN';
  }
}

const i18n = createI18n({
  legacy: false,
  locale: readStoredLocale(),
  fallbackLocale: 'en-US',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
});

export default i18n;
