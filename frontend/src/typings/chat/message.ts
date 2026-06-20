export interface IGeneralUnderstandingRequest {
  uid: string;
  session: string;
  message: string;
}

export interface IGeneralUnderstandingResponse {
  urgency: string;
  explanation: string;
  advice: string;
  disclaimer: string;
  guideReferences?: string[];
}

export interface IChatMessageResponse {
  id: number;
  role: string;
  content: string;
  urgency?: string | null;
  explanation?: string | null;
  advice?: string | null;
  disclaimer?: string | null;
  guideReferences?: string[] | null;
  createdAt: string;
}

export interface IFetchMessagesParams {
  uid: string;
  session: string;
  beforeId?: number;
  pageSize?: number;
}

export interface IChatMessagePageResponse {
  records: IChatMessageResponse[];
  total: number;
  pageSize: number;
  hasMore: boolean;
}
