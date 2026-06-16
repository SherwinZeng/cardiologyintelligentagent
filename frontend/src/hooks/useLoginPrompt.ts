import { ref } from 'vue'

export type LoginPromptChoice = 'login' | 'guest' | 'cancel'

const visible = ref(false)
let pendingResolve: ((choice: LoginPromptChoice) => void) | null = null

export function useLoginPrompt() {
  function openLoginPrompt(): Promise<LoginPromptChoice> {
    visible.value = true
    return new Promise((resolve) => {
      pendingResolve = resolve
    })
  }

  function resolveLoginPrompt(choice: LoginPromptChoice) {
    visible.value = false
    pendingResolve?.(choice)
    pendingResolve = null
  }

  return {
    visible,
    openLoginPrompt,
    resolveLoginPrompt,
  }
}
