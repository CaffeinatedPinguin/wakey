import { createRoute } from '@tanstack/react-router'
import { ChecksPage, validateChecksSearch } from '../features/checks'
import { panelRoute } from './panel'

export const checksRoute = createRoute({
  getParentRoute: () => panelRoute,
  path: '/checks',
  validateSearch: validateChecksSearch,
  component: ChecksPage,
})
