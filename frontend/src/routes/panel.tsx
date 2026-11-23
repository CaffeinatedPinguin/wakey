import { createRoute, redirect } from '@tanstack/react-router'
import { PanelShell } from '../app/shell/PanelShell'
import { meQueryOptions } from '../features/auth'
import { isUnauthorizedError } from '../shared/api/client'
import { rootRoute } from './__root'

export const panelRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: 'panel',
  beforeLoad: async ({ context }) => {
    try {
      await context.queryClient.query({ ...meQueryOptions, staleTime: 'static' })
    } catch (error) {
      if (isUnauthorizedError(error)) {
        redirect({ to: '/login', throw: true })
      }
      throw error
    }
  },
  component: PanelShell,
})
