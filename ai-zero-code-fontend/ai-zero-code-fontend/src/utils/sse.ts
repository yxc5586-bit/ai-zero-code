import { API_BASE_URL } from '@/request'

export type CodeGenerationStreamOptions = {
  appId: string
  message: string
  onChunk: (chunk: string) => void
  onDone: () => void
  onError: (error: Error) => void
}

export type CodeGenerationStream = {
  close: () => void
}

export const openCodeGenerationStream = (
  options: CodeGenerationStreamOptions,
): CodeGenerationStream => {
  const query = new URLSearchParams({ appId: options.appId, message: options.message })
  const eventSource = new window.EventSource(`${API_BASE_URL}/app/chat/gen/code?${query.toString()}`, {
    withCredentials: true,
  })
  let finished = false

  eventSource.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data) as { d?: string }
      if (typeof payload.d === 'string') {
        options.onChunk(payload.d)
      }
    } catch {
      options.onChunk(event.data)
    }
  }

  eventSource.addEventListener('done', () => {
    finished = true
    eventSource.close()
    options.onDone()
  })

  eventSource.onerror = () => {
    if (finished) return
    eventSource.close()
    options.onError(new Error('生成连接中断，请检查服务状态后重试'))
  }

  return {
    close: () => {
      finished = true
      eventSource.close()
    },
  }
}
