import { MantineProvider } from '@mantine/core'
import { QueryClientProvider } from '@tanstack/react-query'
import type { QueryClient } from '@tanstack/react-query'
import { RouterProvider } from '@tanstack/react-router'
import type { ReactElement } from 'react'
import './i18n'
import type { AppRouter } from './router'

interface AppProps {
  router: AppRouter
  queryClient: QueryClient
}

export default function App({ router, queryClient }: AppProps): ReactElement {
  return (
    <MantineProvider defaultColorScheme="auto">
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </MantineProvider>
  )
}
