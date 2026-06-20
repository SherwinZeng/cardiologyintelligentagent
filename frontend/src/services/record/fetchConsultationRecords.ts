import http from '@/http';

export interface IFetchConsultationRecordsParams {
  uid: string;
  page: number;
  pageSize: number;
  urgency?: string;
  keyword?: string;
  startDate?: string;
  endDate?: string;
}

export async function fetchConsultationRecordsService<T>(
  params: IFetchConsultationRecordsParams,
): Promise<T> {
  return http.get<T, T>('/chat/record/list/v1', {
    params,
    timeout: 15000,
  });
}
