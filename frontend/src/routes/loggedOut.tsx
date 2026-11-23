import { createRoute } from '@tanstack/react-router'
import { LoggedOutScreen } from '../features/auth'
import { rootRoute } from './__root'

export const loggedOutRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/logged-out',
  component: LoggedOutScreen,
})
