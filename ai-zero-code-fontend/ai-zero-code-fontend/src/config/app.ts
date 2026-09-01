const normalizeBaseUrl = (value: string) => value.trim().replace(/\/+$/, '')

const apiBaseUrl = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL || '/api')
const configuredDeployBaseUrl =
  import.meta.env.VITE_DEPLOY_DOMAIN || import.meta.env.VITE_APP_DEPLOY_BASE_URL
const productionDeployBaseUrl = `${window.location.origin}${import.meta.env.BASE_URL}deploy`

/** 已部署应用的域名，不包含 deployKey。 */
export const APP_DEPLOY_BASE_URL = normalizeBaseUrl(
  import.meta.env.PROD
    ? productionDeployBaseUrl
    : configuredDeployBaseUrl || 'http://localhost:8080/deploy',
)

/** 生成应用的静态预览根地址，不包含 codeGenType_appId。 */
export const APP_PREVIEW_BASE_URL = `${apiBaseUrl}/static`
