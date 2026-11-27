import { createRouter } from '@tanstack/react-router'
import type { RouterHistory } from '@tanstack/react-router'
import type { QueryClient } from '@tanstack/react-query'
import { rootRoute } from '../routes/__root'
import { checkDetailsRoute } from '../routes/checkDetails'
import { checksRoute } from '../routes/checks'
import { homeRoute } from '../routes/home'
import { incidentsRoute } from '../routes/incidents'
import { loggedOutRoute } from '../routes/loggedOut'
import { loginRoute } from '../routes/login'
import { panelRoute } from '../routes/panel'
import { publicRoute } from '../routes/public'
import { settingsRoute } from '../routes/settings'
import { statusPagesRoute } from '../routes/statusPages'

const routeTree = rootRoute.addChildren([
  panelRoute.addChildren([
    homeRoute,
    checksRoute,
    checkDetailsRoute,
    incidentsRoute,
    statusPagesRoute,
    settingsRoute,
  ]),
  publicRoute.addChildren([loginRoute, loggedOutRoute]),
])

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
