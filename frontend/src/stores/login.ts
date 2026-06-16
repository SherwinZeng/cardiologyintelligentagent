import { defineStore } from 'pinia'

import { useLocalStorage } from '@/hooks/useStorage'
import type { ILoginState } from '@/typings/state/loginState.ts'
import { resolveUserDisplayName } from '@/utils/resolveUserDisplayName.ts'

const STORAGE_KEY = 'user_login'

export const useUserLoginStore = defineStore('userLogin', () => {
  const userLoginStore = reactive<ILoginState>({
    username: '',
    phone: '',
    id: '',
    token: '',
    avatar: '',
  })

  const isLoggedIn = computed(() => Boolean(userLoginStore.token && userLoginStore.id))

  const displayName = computed(() => {
    if (userLoginStore.username === '访客') {
      return '访客'
    }
    return resolveUserDisplayName(userLoginStore.username, userLoginStore.phone)
  })

  const changeUserLoginStore = (loginState: ILoginState) => {
    userLoginStore.username = loginState.username
    userLoginStore.phone = loginState.phone
    userLoginStore.id = loginState.id
    userLoginStore.token = loginState.token
    userLoginStore.avatar = loginState.avatar
  }

  /** 刷新页面后从 localStorage 恢复登录态 */
  const init = () => {
    const { getItem } = useLocalStorage()
    const raw = getItem(STORAGE_KEY)
    if (!raw) {
      return
    }

    try {
      const data = JSON.parse(raw) as Partial<ILoginState>
      if (!data.token || !data.id) {
        return
      }

      changeUserLoginStore({
        id: data.id,
        token: data.token,
        username: data.username ?? '',
        phone: data.phone ?? '',
        avatar: data.avatar ?? '',
      })
    } catch {
      // ignore invalid cache
    }
  }

  const persistLogin = (loginState: ILoginState) => {
    const { setItem } = useLocalStorage()
    changeUserLoginStore(loginState)
    setItem(
      STORAGE_KEY,
      JSON.stringify({
        id: loginState.id,
        token: loginState.token,
        username: loginState.username,
        phone: loginState.phone,
        avatar: loginState.avatar,
      }),
    )
  }

  const clearLogin = () => {
    const { removeItem } = useLocalStorage()
    changeUserLoginStore({
      username: '',
      phone: '',
      id: '',
      token: '',
      avatar: '',
    })
    removeItem(STORAGE_KEY)
  }

  return {
    userLoginStore,
    isLoggedIn,
    displayName,
    changeUserLoginStore,
    persistLogin,
    clearLogin,
    init,
  }
})
