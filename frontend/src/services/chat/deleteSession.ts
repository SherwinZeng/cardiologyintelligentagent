import http from '@/http';

export interface IDeleteSessionParams {
  uid: string;
  session: string;
}

export async function deleteSessionService<T>(params: IDeleteSessionParams): Promise<T> {
  return http.delete<T, T>('/chat/session/v1', {
    params,
    timeout: 15000,
  });
}
