import type { Ref } from 'vue'

export interface IUseAsyncActionReturn {
    loading: Ref<boolean>
    run: <T>(
        task: () => Promise<T>,
        onSuccess?: (data: T) => void,
        onError?: (error: unknown) => void,
    ) => Promise<T | undefined>
}
