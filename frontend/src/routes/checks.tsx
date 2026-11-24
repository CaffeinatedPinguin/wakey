import { createRoute } from '@tanstack/react-router'
import { ChecksSectionView } from '../app/shell/sectionViews'
import { panelRoute } from './panel'

export const checksRoute = createRoute({
  getParentRoute: () => panelRoute,
  path: '/checks',
  component: ChecksSectionView,
})
