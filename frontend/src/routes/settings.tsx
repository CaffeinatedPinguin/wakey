import { createRoute } from '@tanstack/react-router'
import { SettingsSectionView } from '../app/shell/sectionViews'
import { panelRoute } from './panel'

export const settingsRoute = createRoute({
  getParentRoute: () => panelRoute,
  path: '/settings',
  component: SettingsSectionView,
})
