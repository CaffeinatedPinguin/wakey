import { createRoute } from '@tanstack/react-router'
import { PublicShell } from '../app/shell/PublicShell'
import { rootRoute } from './__root'

export const publicRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: 'public',
  component: PublicShell,
})
