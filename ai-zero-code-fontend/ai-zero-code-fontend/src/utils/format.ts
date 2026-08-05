export const formatDateTime = (value?: string) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export const formatRelativeTime = (value?: string) => {
  if (!value) return '刚刚'
  const timestamp = new Date(value).getTime()
  if (Number.isNaN(timestamp)) return formatDateTime(value)

  const seconds = Math.round((timestamp - Date.now()) / 1000)
  const formatter = new Intl.RelativeTimeFormat('zh-CN', { numeric: 'auto' })
  const ranges: Array<[Intl.RelativeTimeFormatUnit, number]> = [
    ['year', 60 * 60 * 24 * 365],
    ['month', 60 * 60 * 24 * 30],
    ['week', 60 * 60 * 24 * 7],
    ['day', 60 * 60 * 24],
    ['hour', 60 * 60],
    ['minute', 60],
  ]

  for (const [unit, size] of ranges) {
    if (Math.abs(seconds) >= size) {
      return formatter.format(Math.round(seconds / size), unit)
    }
  }

  return '刚刚'
}

export const getDeployUrl = (deployKey?: string) =>
  deployKey ? `${APP_DEPLOY_BASE_URL}/${deployKey}/` : ''

export const getStaticPreviewUrl = (app: Pick<API.AppVO, 'id' | 'codeGenType'>) => {
  if (!app.id || !app.codeGenType) return ''
  return `${APP_PREVIEW_BASE_URL}/${app.codeGenType}_${app.id}/`
}

export const openExternalUrl = (url?: string) => {
  if (url) window.open(url, '_blank', 'noopener,noreferrer')
}
import { APP_DEPLOY_BASE_URL, APP_PREVIEW_BASE_URL } from '@/config/app'
