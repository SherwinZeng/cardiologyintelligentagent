import http from '@/http'
import type { IFetchMessagesParams } from '@/typings/chat/message.ts'

export async function fetchMessagesService<T>(params: IFetchMessagesParams): Promise<T> {
  return http.get<T, T>('/chat/messages/v1', {
    params,
    timeout: 15000,
  })
}
