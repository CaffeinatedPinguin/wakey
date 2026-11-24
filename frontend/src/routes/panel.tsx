import { createRoute, redirect, retainSearchParams } from '@tanstack/react-router'
import { PanelShell } from '../app/shell/PanelShell'
import { meQueryOptions } from '../features/auth'
import { validateSpaceSearch } from '../features/spaces'
import { isUnauthorizedError } from '../shared/api/client'
import { rootRoute } from './__root'

export const panelRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: 'panel',
  validateSearch: validateSpaceSearch,
  search: {
    middlewares: [retainSearchParams(['space'])],
  },
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
