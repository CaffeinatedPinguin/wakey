import {
  IconActivity,
  IconAlertTriangle,
  IconLayoutDashboard,
  IconSettings,
  IconWorld,
} from '@tabler/icons-react'
import type { CSSProperties } from 'react'

export const overviewSection = {
  to: '/',
  labelKey: 'common.nav.overview',
  Icon: IconLayoutDashboard,
  mobile: 'bar',
} as const

export const checksSection = {
  to: '/checks',
  labelKey: 'common.nav.checks',
  Icon: IconActivity,
  mobile: 'bar',
} as const

export const incidentsSection = {
  to: '/incidents',
  labelKey: 'common.nav.incidents',
  Icon: IconAlertTriangle,
  mobile: 'bar',
} as const

export const statusPagesSection = {
  to: '/status-pages',
  labelKey: 'common.nav.statusPages',
  Icon: IconWorld,
  mobile: 'more',
} as const

export const settingsSection = {
  to: '/settings',
  labelKey: 'common.nav.settings',
  Icon: IconSettings,
  mobile: 'more',
} as const

export const panelNavigation = [
  overviewSection,
  checksSection,
  incidentsSection,
  statusPagesSection,
  settingsSection,
] as const

export type PanelSection = (typeof panelNavigation)[number]

export const barNavigation = panelNavigation.filter((section) => section.mobile === 'bar')

export const moreNavigation = panelNavigation.filter((section) => section.mobile === 'more')

export const navigationActiveStyle: CSSProperties = {
  color: 'var(--app-color-nav-active-text)',
  background: 'var(--app-color-nav-active-bg)',
}

export function navigationActiveOptions(to: string): { exact: boolean } {
  return { exact: to === '/' }
}
