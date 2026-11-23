interface ImportMetaEnv {
  readonly VITE_API_BASE_PATH: string
  readonly VITE_LOGIN_PATH: string
  readonly VITE_LOGOUT_PATH: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
