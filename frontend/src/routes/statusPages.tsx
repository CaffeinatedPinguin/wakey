import { createRoute } from '@tanstack/react-router'
import { StatusPagesSectionView } from '../app/shell/sectionViews'
import { panelRoute } from './panel'

export const statusPagesRoute = createRoute({
  getParentRoute: () => panelRoute,
  path: '/status-pages',
  component: StatusPagesSectionView,
})
