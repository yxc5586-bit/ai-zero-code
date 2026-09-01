import { APP_DEPLOY_BASE_URL, APP_PREVIEW_BASE_URL } from '@/config/app'
import { CodeGenTypeEnum } from '@/types/codeGenType'

const CODE_GEN_TYPE_LABELS: Record<string, string> = {
  [CodeGenTypeEnum.HTML]: '原生 HTML 模式',
  [CodeGenTypeEnum.MULTI_FILE]: '原生多文件模式',
  [CodeGenTypeEnum.VUE_PROJECT]: 'Vue 工程模式',
}

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

export const formatCodeGenType = (value?: string) => {
  return value ? CODE_GEN_TYPE_LABELS[value] || value : '—'
}

export const getDeployUrl = (deployKey?: string, deployUrl?: string) =>
  deployUrl?.trim() || (deployKey ? `${APP_DEPLOY_BASE_URL}/${deployKey}/` : '')

export const getStaticPreviewUrl = (app: Pick<API.AppVO, 'id' | 'codeGenType'>) => {
  if (!app.id || !app.codeGenType) return ''
  const distPath = app.codeGenType === CodeGenTypeEnum.VUE_PROJECT ? 'dist/index.html' : ''
  return `${APP_PREVIEW_BASE_URL}/${app.codeGenType}_${app.id}/${distPath}`
}

export const openExternalUrl = (url?: string) => {
  if (url) window.open(url, '_blank', 'noopener,noreferrer')
}
