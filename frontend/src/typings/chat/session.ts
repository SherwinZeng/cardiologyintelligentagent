export interface ICreateSessionRequest {
  uid: string
  session: string
}

export interface ICreateSessionResponse {
  sessionId: string
  uid: string
  title: string
  preview: string
  messageCount: number
  status: string
  createdAt: string
  updatedAt: string
}
