import { MantineProvider } from '@mantine/core'
import { QueryClientProvider } from '@tanstack/react-query'
import type { QueryClient } from '@tanstack/react-query'
import { RouterProvider } from '@tanstack/react-router'
import type { ReactElement } from 'react'
import './i18n'
import { appQueryClient } from './queryClient'
import { appRouter } from './router'
import type { AppRouter } from './router'
import { theme } from './theme'

interface AppProps {
  router?: AppRouter
  queryClient?: QueryClient
}

export default function App({
  router = appRouter,
  queryClient = appQueryClient,
}: AppProps): ReactElement {
  return (
    <MantineProvider theme={theme}>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </MantineProvider>
  )
}
