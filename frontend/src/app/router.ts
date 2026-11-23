import { createRouter } from '@tanstack/react-router'
import type { RouterHistory } from '@tanstack/react-router'
import type { QueryClient } from '@tanstack/react-query'
import { rootRoute } from '../routes/__root'
import { homeRoute } from '../routes/home'
import { loggedOutRoute } from '../routes/loggedOut'
import { loginRoute } from '../routes/login'
import { appQueryClient } from './queryClient'

const routeTree = rootRoute.addChildren([homeRoute, loginRoute, loggedOutRoute])

function buildRouter(queryClient: QueryClient, history?: RouterHistory) {
  if (history === undefined) {
    return createRouter({ routeTree, context: { queryClient } })
  }
  return createRouter({ routeTree, context: { queryClient }, history })
}

export type AppRouter = ReturnType<typeof buildRouter>

export function createAppRouter(queryClient: QueryClient, history?: RouterHistory): AppRouter {
  return buildRouter(queryClient, history)
}

export const appRouter = createAppRouter(appQueryClient)

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof appRouter
  }
}
