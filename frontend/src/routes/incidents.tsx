import { createRoute } from '@tanstack/react-router'
import { IncidentsSectionView } from '../app/shell/sectionViews'
import { panelRoute } from './panel'

export const incidentsRoute = createRoute({
  getParentRoute: () => panelRoute,
  path: '/incidents',
  component: IncidentsSectionView,
})
