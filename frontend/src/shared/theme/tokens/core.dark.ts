import { roleAliases } from './core.roles'

/** Dark scheme colours. */
export const darkTokens = {
  ...roleAliases,
  '--app-color-page': '#121316',
  '--app-color-surface': '#1B1B1F',
  '--app-color-surface-subtle': '#17171B',
  '--app-color-surface-raised': '#232328',
  '--app-color-surface-sidebar': '#18191D',
  '--app-color-surface-navbar': '#18191D',
  '--app-color-text-heading': '#EDEDF1',
  '--app-color-text-body': '#C6C6CE',
  '--app-color-text-muted': '#93939F',
  '--app-color-border-soft': '#2D2D34',
  '--app-color-border-default': '#74747F',
  '--app-color-border-strong': '#74747F',
  '--app-color-interactive-hover': '#26262D',
  '--app-color-interactive-pressed': '#303037',
  '--app-color-input-bg': 'var(--app-color-surface-subtle)',
  '--app-color-disabled-text': '#6E6E79',
  '--app-color-selection-bg':
    'color-mix(in srgb, var(--app-color-accent) 12%, var(--app-color-surface))',
  // 16%, because at 18% the accent text reached exactly 4.50:1 (on the boundary).
  '--app-color-selection-hover':
    'color-mix(in srgb, var(--app-color-accent) 16%, var(--app-color-surface))',
  '--app-color-nav-active-bg': 'color-mix(in srgb, var(--app-color-accent) 12%, transparent)',
  '--app-color-nav-active-hover': 'color-mix(in srgb, var(--app-color-accent) 16%, transparent)',

  '--app-color-danger-bg': '#B63A3B',
  '--app-color-danger-hover': '#9C2F30',
  '--app-color-danger-pressed': '#842728',
  '--app-color-danger-text': '#FF8586',

  /* General statuses (messages, validation, domains). */
  '--app-color-status-success-solid': '#10B981',
  '--app-color-status-success-text': '#86EFAC',
  '--app-color-status-success-bg': 'color-mix(in srgb, #10B981 16%, var(--app-color-surface))',
  '--app-color-status-success-on-solid': '#0B0F17',
  '--app-color-status-warning-solid': '#F59E0B',
  '--app-color-status-warning-text': '#FCD34D',
  '--app-color-status-warning-bg': 'color-mix(in srgb, #F59E0B 16%, var(--app-color-surface))',
  '--app-color-status-warning-on-solid': '#0B0F17',
  '--app-color-status-danger-solid': '#F43F5E',
  '--app-color-status-danger-text': '#FCA5A5',
  '--app-color-status-danger-bg': 'color-mix(in srgb, #F43F5E 16%, var(--app-color-surface))',
  '--app-color-status-danger-on-solid': '#0B0F17',
  '--app-color-status-info-solid': '#3B82F6',
  '--app-color-status-info-text': '#93C5FD',
  '--app-color-status-info-bg': 'color-mix(in srgb, #3B82F6 16%, var(--app-color-surface))',
  '--app-color-status-info-on-solid': '#0B0F17',
  '--app-color-status-neutral-solid': '#94A3B8',
  '--app-color-status-neutral-text': '#CBD5E1',
  '--app-color-status-neutral-bg': 'color-mix(in srgb, #94A3B8 16%, var(--app-color-surface))',
  '--app-color-status-neutral-on-solid': '#0B0F17',

  '--app-color-overlay-backdrop': 'rgb(0 0 0 / 0.56)',
  '--app-shadow-popover': '0 8px 24px rgb(0 0 0 / 0.28)',
  '--app-shadow-overlay': '0 16px 48px rgb(0 0 0 / 0.32)',

  '--app-chart-grid': '#2D2D34',
  '--app-chart-axis': '#93939F',
  '--app-chart-threshold': '#F59E0B',
  '--app-chart-series-1': 'var(--app-color-accent-text)',
  '--app-chart-series-2': '#3B82F6',
  '--app-chart-series-3': '#10B981',
  '--app-chart-series-4': '#F59E0B',
} as const
