import type { QueryClient } from '@tanstack/react-query'
import type { RouterHistory } from '@tanstack/react-router'
import { createQueryClient } from './queryClient'
import { createAppRouter } from './router'
import type { AppRouter } from './router'

export interface AppServices {
  router: AppRouter
  queryClient: QueryClient
}

// The router and the query client depend on each other: the router puts the
// client in its context, and the client needs the router to leave an expired
// session. Build the client first with a holder that is filled once the
// router exists.
export function createAppServices(history?: RouterHistory): AppServices {
  const holder: { router?: AppRouter } = {}
  const queryClient = createQueryClient(() => {
    void holder.router?.navigate({ to: '/login' })
  })
  const router = createAppRouter(queryClient, history)
  holder.router = router
  return { router, queryClient }
}
