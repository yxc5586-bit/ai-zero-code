export const isApiSuccess = (response: API.BaseResponse<unknown>) => response.code === 0

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
