import { ElMessage } from 'element-plus';
import { storeToRefs } from 'pinia';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

import { useUserLoginStore } from '@/stores/login.ts';
import { buildLoginRoute, sanitizeLoginRedirect } from '@/utils/resolveLoginRedirect.ts';

interface RequireAuthOptions {
  /** 登录成功后回跳地址，默认当前页 */
  redirect?: string;
  /** 跳转前是否提示 */
  notify?: boolean;
}

/**
 * 操作级鉴权：页面可浏览，仅在需要写操作/调用接口时要求登录。
 */
export function useRequireAuth() {
  const { t } = useI18n();
  const route = useRoute();
  const router = useRouter();
  const loginStore = useUserLoginStore();
  const { isLoggedIn } = storeToRefs(loginStore);

  function requireAuth(options: RequireAuthOptions = {}): boolean {
    if (isLoggedIn.value) {
      return true;
    }

    const redirect = sanitizeLoginRedirect(options.redirect ?? route.fullPath);
    if (options.notify !== false) {
      ElMessage.info(t('auth.loginRequired'));
    }

    void router.push(buildLoginRoute(redirect));
    return false;
  }

  return { requireAuth, isLoggedIn };
}
