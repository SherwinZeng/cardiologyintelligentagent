import type { ChatMessage, ChatSessionItem, UrgencyLevel } from '@/views/chat/types'
import type { IChatMessageResponse, IChatMessagePageResponse } from '@/typings/chat/message.ts'
import type { ICreateSessionResponse, IChatSessionPageResponse } from '@/typings/chat/session.ts'

function formatMessageTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

export function mapUrgencyLevel(urgency?: string | null): UrgencyLevel | undefined {
  if (!urgency) {
    return undefined
  }

  const normalized = urgency.toLowerCase()
  if (normalized === 'green' || normalized === 'low') {
    return 'low'
  }
  if (normalized === 'yellow' || normalized === 'moderate') {
    return 'moderate'
  }
  if (normalized === 'red' || normalized === 'high') {
    return 'high'
  }
  return undefined
}

export function toSessionItem(data: ICreateSessionResponse): ChatSessionItem {
  return {
    id: data.sessionId,
    title: data.title,
    preview: data.preview,
    time: formatMessageTime(data.updatedAt),
    messageCount: data.messageCount,
    status: data.status,
    pinned: Boolean(data.pinned),
    createdAt: data.createdAt,
    updatedAt: data.updatedAt,
  }
}

export function parseSessionPageData(
  payload: IChatSessionPageResponse | ICreateSessionResponse[] | null | undefined,
): IChatSessionPageResponse {
  if (!payload) {
    return { records: [], total: 0, page: 1, pageSize: 5, hasMore: false }
  }
  if (Array.isArray(payload)) {
    return {
      records: payload,
      total: payload.length,
      page: 1,
      pageSize: payload.length || 5,
      hasMore: false,
    }
  }
  return {
    records: Array.isArray(payload.records) ? payload.records : [],
    total: payload.total ?? 0,
    page: payload.page ?? 1,
    pageSize: payload.pageSize ?? 5,
    hasMore: Boolean(payload.hasMore),
  }
}

export function toSessionItemList(data: ICreateSessionResponse[] | null | undefined): ChatSessionItem[] {
  if (!Array.isArray(data)) {
    return []
  }
  const items: ChatSessionItem[] = []
  for (const session of data) {
    items.push(toSessionItem(session))
  }
  return items
}

export function parseMessagePageData(
  payload: IChatMessagePageResponse | IChatMessageResponse[] | null | undefined,
): IChatMessagePageResponse {
  if (!payload) {
    return { records: [], total: 0, pageSize: 40, hasMore: false }
  }
  if (Array.isArray(payload)) {
    return {
      records: payload,
      total: payload.length,
      pageSize: payload.length || 40,
      hasMore: false,
    }
  }
  return {
    records: Array.isArray(payload.records) ? payload.records : [],
    total: payload.total ?? 0,
    pageSize: payload.pageSize ?? 40,
    hasMore: Boolean(payload.hasMore),
  }
}

export function toChatMessageList(data: IChatMessageResponse[] | null | undefined): ChatMessage[] {
  if (!Array.isArray(data)) {
    return []
  }
  const items: ChatMessage[] = []
  for (const item of data) {
    items.push(toChatMessage(item))
  }
  return items
}

export function toChatMessage(data: IChatMessageResponse): ChatMessage {
  if (data.role === 'assistant') {
    return {
      id: String(data.id),
      role: 'assistant',
      content: data.content,
      time: formatMessageTime(data.createdAt),
      urgency: mapUrgencyLevel(data.urgency),
      sections: {
        advice: data.advice ?? undefined,
        notes: data.disclaimer ?? undefined,
      },
    }
  }

  return {
    id: String(data.id),
    role: 'user',
    content: data.content,
    time: formatMessageTime(data.createdAt),
  }
}
