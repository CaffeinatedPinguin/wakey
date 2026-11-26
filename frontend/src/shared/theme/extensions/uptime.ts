/**
 * Extension: uptime monitor states.
 *
 * The five base states alias the general core statuses, so there is one
 * palette and the brand colour never overrides them.
 */
import type { ThemeExtension } from '../tokens/extension'
import { rem } from '../tokens/types'

const state = (name: string, status: string): Record<`--${string}`, string> => ({
  [`--app-state-${name}-solid`]: `var(--app-color-status-${status}-solid)`,
  [`--app-state-${name}-text`]: `var(--app-color-status-${status}-text)`,
  [`--app-state-${name}-bg`]: `var(--app-color-status-${status}-bg)`,
  [`--app-state-${name}-on-solid`]: `var(--app-color-status-${status}-on-solid)`,
})

const states = {
  ...state('up', 'success'),
  ...state('degraded', 'warning'),
  ...state('down', 'danger'),
  ...state('unknown', 'neutral'),
  ...state('maintenance', 'info'),
  // Checks that do not measure (paused, configuring, waiting).
  '--app-state-idle-icon': 'var(--app-color-text-muted)',
  // No data on the history bar: an empty segment with an outline ≥ 3:1.
  '--app-state-nodata-fill': 'transparent',
  '--app-state-nodata-border': 'var(--app-color-border-control)',
  // A day marker with an incident: a non-colour carrier of information.
  '--app-state-marker': 'var(--app-color-text-heading)',
} as const

export const uptimeExtension: ThemeExtension = {
  name: 'uptime',
  shared: {
    '--app-layout-statuspage-max': rem(880),
    '--app-layout-feed-width': rem(320),
    '--app-layout-aside-width': rem(320),
    '--app-size-history-bar': rem(32),
  },
  light: {
    ...states,
    // Instance administration: a separate accent, 5.26:1 on its background in
    // the light scheme and 6.63:1 in the dark scheme.
    '--app-color-admin-accent': '#A04A06',
    '--app-color-admin-bg': 'color-mix(in srgb, #B45309 8%, transparent)',
  },
  dark: {
    ...states,
    '--app-color-admin-accent': '#F59E0B',
    '--app-color-admin-bg': 'color-mix(in srgb, #F59E0B 12%, transparent)',
  },
  semanticColors: {
    up: '--app-state-up',
    degraded: '--app-state-degraded',
    down: '--app-state-down',
    unknown: '--app-state-unknown',
    maintenance: '--app-state-maintenance',
  },
}
