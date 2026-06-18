import http from '@/http';

export interface IFetchSessionsParams {
  uid: string;
  page: number;
  pageSize: number;
  keyword?: string;
}

export async function fetchSessionsService<T>(params: IFetchSessionsParams): Promise<T> {
  return http.get<T, T>('/chat/session/list/v1', {
    params,
    timeout: 15000,
  });
}
