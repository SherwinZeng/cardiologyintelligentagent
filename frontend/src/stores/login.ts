import { defineStore } from 'pinia'

import { useLocalStorage } from '@/hooks/useStorage'
import type { ILoginState } from '@/typings/state/loginState.ts'

const STORAGE_KEY = 'user_login'

export const useUserLoginStore = defineStore('userLogin', () => {
  const userLoginStore = reactive<ILoginState>({
    username: '',
    id: '',
    token: '',
    avatar: '',
  })

  const isLoggedIn = computed(() => Boolean(userLoginStore.token && userLoginStore.id))

  const displayName = computed(() => {
    if (userLoginStore.username) {
      return userLoginStore.username
    }
    return isLoggedIn.value ? '访客' : ''
  })

  const changeUserLoginStore = (loginState: ILoginState) => {
    userLoginStore.username = loginState.username
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
        username: data.username ?? '访客',
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
        avatar: loginState.avatar,
      }),
    )
  }

  const clearLogin = () => {
    const { removeItem } = useLocalStorage()
    changeUserLoginStore({
      username: '',
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
