import { createRoute } from '@tanstack/react-router'
import { CheckDetailsPage } from '../features/checks'
import { panelRoute } from './panel'

export const checkDetailsRoute = createRoute({
  getParentRoute: () => panelRoute,
  path: '/checks/$checkId',
  component: CheckDetailsPage,
})
