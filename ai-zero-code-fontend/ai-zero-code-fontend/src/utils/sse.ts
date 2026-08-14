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
    if (finished) return

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
    if (finished) return

    finished = true
    eventSource.close()
    options.onDone()
  })

  eventSource.addEventListener('business-error', (event) => {
    if (finished) return

    try {
      const payload = JSON.parse((event as MessageEvent<string>).data) as { message?: string }
      const errorMessage = payload.message || '生成过程中出现错误'
      finished = true
      eventSource.close()
      options.onError(new Error(errorMessage))
    } catch {
      finished = true
      eventSource.close()
      options.onError(new Error('服务器返回错误'))
    }
  })

  eventSource.onerror = () => {
    if (finished) return
    finished = true
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
