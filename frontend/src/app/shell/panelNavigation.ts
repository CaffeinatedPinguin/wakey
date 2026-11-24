import { IconLayoutDashboard } from '@tabler/icons-react'
import type { CSSProperties } from 'react'

export const panelNavigation = [
  {
    to: '/',
    labelKey: 'common.nav.overview',
    Icon: IconLayoutDashboard,
  },
] as const

export const navigationActiveStyle: CSSProperties = {
  color: 'var(--app-color-nav-active-text)',
  background: 'var(--app-color-nav-active-bg)',
}

export function navigationActiveOptions(to: string): { exact: boolean } {
  return { exact: to === '/' }
}
