import { MantineProvider } from '@mantine/core'
import { QueryClientProvider } from '@tanstack/react-query'
import type { QueryClient } from '@tanstack/react-query'
import { RouterProvider } from '@tanstack/react-router'
import { useMemo } from 'react'
import type { ReactElement, ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { createAppTheme, uptimeExtension } from '../shared/theme'
import './i18n'
import type { AppRouter } from './router'

interface AppProps {
  router: AppRouter
  queryClient: QueryClient
}

interface AppThemeProviderProps {
  children: ReactNode
}

export function AppThemeProvider({ children }: AppThemeProviderProps): ReactElement {
  const { t } = useTranslation()
  // Mantine skips transition timers in tests, so menu content is available right after it opens.
  const testEnv = import.meta.env.MODE === 'test' ? { env: 'test' as const } : {}
  const appTheme = useMemo(
    () =>
      createAppTheme({
        extensions: [uptimeExtension],
        labels: { close: t('common.close') },
      }),
    [t],
  )
  return (
    <MantineProvider
      {...testEnv}
      theme={appTheme.theme}
      cssVariablesResolver={appTheme.cssVariablesResolver}
      defaultColorScheme="auto"
    >
      {children}
    </MantineProvider>
  )
}

export default function App({ router, queryClient }: AppProps): ReactElement {
  return (
    <AppThemeProvider>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </AppThemeProvider>
  )
}
