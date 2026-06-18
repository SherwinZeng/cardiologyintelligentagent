import http from '@/http';

export async function handleCreatSessionService<T>(data: unknown): Promise<T> {
  return http.post<T, T>('/chat/session/create', data).then(
    (response: T) => response,
    (error) => Promise.reject(error),
  );
}
