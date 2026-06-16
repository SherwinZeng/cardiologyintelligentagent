import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'

import { useLoginPrompt } from '@/hooks/useLoginPrompt.ts'
import { useUserLoginStore } from '@/stores/login.ts'
import { buildLoginRoute } from '@/utils/resolveLoginRedirect.ts'

/**
 * 未登录时弹出「去登录 / 游客体验」选择框，避免直接硬跳转。
 */
export function useEnsureAuthWithPrompt() {
  const route = useRoute()
  const router = useRouter()
  const loginStore = useUserLoginStore()
  const { isLoggedIn } = storeToRefs(loginStore)
  const { openLoginPrompt } = useLoginPrompt()

  async function ensureAuthWithPrompt(): Promise<boolean> {
    if (isLoggedIn.value) {
      return true
    }

    const choice = await openLoginPrompt()
    if (choice === 'guest') {
      return isLoggedIn.value
    }
    if (choice === 'login') {
      void router.push(buildLoginRoute(route.fullPath))
    }
    return false
  }

  return { ensureAuthWithPrompt }
}
