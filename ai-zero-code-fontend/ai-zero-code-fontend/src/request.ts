import axios from 'axios'
import { message } from 'ant-design-vue'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const myAxios = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
  withCredentials: true,
})

myAxios.interceptors.response.use(
  (response) => {
    const responseData = response.data as API.BaseResponse<unknown> | undefined

    if (responseData?.code === 40100) {
      const responseUrl = String(response.request?.responseURL ?? '')
      const isLoginProbe = responseUrl.includes('/user/get/login')
      const isAuthPage = window.location.pathname.startsWith('/user/')

      if (!isLoginProbe && !isAuthPage) {
        const redirect = encodeURIComponent(
          `${window.location.pathname}${window.location.search}${window.location.hash}`,
        )
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${redirect}`
      }
    }

    return response
  },
  (error) => Promise.reject(error),
)

export default myAxios
