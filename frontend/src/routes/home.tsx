import { createRoute } from '@tanstack/react-router'
import { HomeView } from '../features/auth'
import { panelRoute } from './panel'

export const homeRoute = createRoute({
  getParentRoute: () => panelRoute,
  path: '/',
  component: HomeView,
})
