import { createRoute } from '@tanstack/react-router'
import { LoggedOutScreen } from '../features/auth'
import { publicRoute } from './public'

export const loggedOutRoute = createRoute({
  getParentRoute: () => publicRoute,
  path: '/logged-out',
  component: LoggedOutScreen,
})
