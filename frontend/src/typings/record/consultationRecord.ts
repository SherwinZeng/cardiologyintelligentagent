export interface IConsultationRecordResponse {
  id: number;
  sessionId: string;
  uid: string;
  title: string;
  urgency: 'green' | 'yellow' | 'red' | string;
  summary: string;
  messageCount: number;
  startedAt?: string | null;
  endedAt?: string | null;
  generatedAt?: string | null;
}

export interface IConsultationRecordPageResponse {
  records: IConsultationRecordResponse[];
  total: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}
