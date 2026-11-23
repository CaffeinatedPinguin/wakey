import { z } from 'zod'

// Environment names and their validation live here, separate from the module
// that reads `import.meta.env`, so the same rules can be tested directly.
const serverPath = z.string().regex(/^\/(?!\/)/)

const envSchema = z.object({
  VITE_API_BASE_PATH: serverPath,
  VITE_LOGIN_PATH: serverPath,
  VITE_LOGOUT_PATH: serverPath,
})

export interface AppConfig {
  readonly apiBaseUrl: string
  readonly loginUrl: string
  readonly logoutUrl: string
}

export function parseAppConfig(input: unknown): AppConfig {
  const result = envSchema.safeParse(input)
  if (!result.success) {
    const details = result.error.issues
      .map((issue) => `${issue.path.join('.')}: ${issue.message}`)
      .join('; ')
    throw new Error(`Invalid frontend configuration: ${details}`)
  }

  return Object.freeze({
    apiBaseUrl: result.data.VITE_API_BASE_PATH,
    loginUrl: result.data.VITE_LOGIN_PATH,
    logoutUrl: result.data.VITE_LOGOUT_PATH,
  })
}
