import http from '@/http';

export interface IPinSessionParams {
  uid: string;
  session: string;
  pinned: boolean;
}

export async function pinSessionService<T>(params: IPinSessionParams): Promise<T> {
  return http.post<T, T>('/chat/session/pin/v1', params, {
    timeout: 15000,
  });
}
