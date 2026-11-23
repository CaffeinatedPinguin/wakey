import { createRouter } from '@tanstack/react-router'
import type { RouterHistory } from '@tanstack/react-router'
import type { QueryClient } from '@tanstack/react-query'
import { rootRoute } from '../routes/__root'
import { homeRoute } from '../routes/home'
import { loggedOutRoute } from '../routes/loggedOut'
import { loginRoute } from '../routes/login'

const routeTree = rootRoute.addChildren([homeRoute, loginRoute, loggedOutRoute])

const createRouterForTree = createRouter<typeof routeTree>

export type AppRouter = ReturnType<typeof createRouterForTree>

export function createAppRouter(queryClient: QueryClient, history?: RouterHistory): AppRouter {
  return createRouterForTree({
    routeTree,
    context: { queryClient },
    ...(history === undefined ? {} : { history }),
  })
}

declare module '@tanstack/react-router' {
  interface Register {
    router: AppRouter
  }
}
