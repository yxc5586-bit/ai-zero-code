export const getApiErrorMessage = (error: unknown, fallback = '请求失败，请稍后重试') => {
  if (typeof error === 'object' && error !== null) {
    const maybeAxiosError = error as {
      response?: { data?: { message?: string } }
      message?: string
    }
    return maybeAxiosError.response?.data?.message || maybeAxiosError.message || fallback
  }

  return fallback
}

export const toSafePageNumber = (value: string | number | undefined, fallback = 0) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

/** 移除查询对象中的空字符串和空值，同时统一清理字符串两端空白。 */
export const cleanQueryParams = <T extends object>(source: T): Partial<T> => {
  const result: Partial<T> = {}
  Object.entries(source).forEach(([key, value]) => {
    if (value !== '' && value !== undefined && value !== null) {
      result[key as keyof T] = (typeof value === 'string' ? value.trim() : value) as T[keyof T]
    }
  })
  return result
}
