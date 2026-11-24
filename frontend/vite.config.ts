import react from '@vitejs/plugin-react'
import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'

declare const process: { cwd(): string }

const backend = 'http://localhost:8080'

// Spring's OIDC callback is served under /login/oauth2/code/{registrationId},
// a prefix outside the configured authorization URL. The proxy keeps only that
// prefix so the SPA route /login stays served by the dev server.
const LOGIN_CALLBACK_PREFIX = '/login/oauth2'

function proxyPrefix(url: string): string {
  const segment = url.split('/').find((part) => part !== '') ?? ''
  return `/${segment}`
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [react()],
    server: {
      proxy: {
        [proxyPrefix(env.VITE_API_BASE_PATH)]: backend,
        [proxyPrefix(env.VITE_LOGIN_PATH)]: backend,
        [LOGIN_CALLBACK_PREFIX]: backend,
        [env.VITE_LOGOUT_PATH]: backend,
      },
    },
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
    },
  }
})
