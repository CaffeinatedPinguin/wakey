import { parseAppConfig } from './schema'

export type { AppConfig } from './schema'

// The only module that reads Vite environment variables. Everything else
// imports this frozen object, so addresses live in frontend/.env and a
// missing or invalid value stops the application at start-up.
export const appConfig = parseAppConfig(import.meta.env)
