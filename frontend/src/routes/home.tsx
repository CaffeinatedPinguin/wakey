import { createRoute, redirect } from '@tanstack/react-router'
import { HomeView, meQueryOptions } from '../features/auth'
import { isUnauthorizedError } from '../shared/api/client'
import { rootRoute } from './__root'

export const homeRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
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
  component: HomeView,
})
