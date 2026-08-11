/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_DEPLOY_DOMAIN?: string
  readonly VITE_APP_DEPLOY_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
