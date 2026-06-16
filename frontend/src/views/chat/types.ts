export type ChatRole = 'user' | 'assistant'

export type UrgencyLevel = 'low' | 'moderate' | 'high'

export interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  time: string
  urgency?: UrgencyLevel
  animate?: boolean
  sections?: {
    analysis?: string
    advice?: string
    notes?: string
  }
}

export interface ChatSessionItem {
  id: string
  title: string
  preview: string
  time: string
  messageCount: number
  status: string
  pinned: boolean
  createdAt: string
  updatedAt: string
}
