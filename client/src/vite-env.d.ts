/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_WS_URL: string
  readonly VITE_USE_MOCK_AUTH: string
  readonly VITE_USE_MOCK_DATA: string
  readonly VITE_DEBUG: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
