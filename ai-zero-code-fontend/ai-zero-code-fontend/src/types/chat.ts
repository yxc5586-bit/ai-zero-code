export type ChatMessageStatus = 'done' | 'streaming' | 'error'

export type ChatMessage = {
  id: string
  role: 'user' | 'assistant'
  content: string
  status: ChatMessageStatus
  sourceMessage?: string
  sourceRequestMessage?: string
  createTime?: string
}
