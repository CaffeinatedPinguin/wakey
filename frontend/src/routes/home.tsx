import { createRoute } from '@tanstack/react-router'
import { HomeView } from '../features/auth'
import { rootRoute } from './__root'

export const homeRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: HomeView,
})
