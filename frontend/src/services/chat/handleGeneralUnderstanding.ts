import http from '@/http';
import type { IGeneralUnderstandingRequest } from '@/typings/chat/message.ts';

export async function handleGeneralUnderstandingService<T>(
  data: IGeneralUnderstandingRequest,
): Promise<T> {
  return http.post<T, T>('/chat/generalUnderstanding/v1', data, {
    timeout: 120000,
  });
}
