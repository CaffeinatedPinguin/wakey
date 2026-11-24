import { roleAliases } from './core.roles'

/** Light scheme colours. */
export const lightTokens = {
  ...roleAliases,
  '--app-color-page': '#F6F6F8',
  '--app-color-surface': '#FFFFFF',
  '--app-color-surface-subtle': '#F1F1F4',
  '--app-color-surface-raised': '#FFFFFF',
  '--app-color-surface-sidebar': '#FBFBFC',
  '--app-color-surface-navbar': '#FFFFFF',
  '--app-color-text-heading': '#1C1C23',
  '--app-color-text-body': '#3D3D48',
  '--app-color-text-muted': '#62626F',
  '--app-color-border-soft': '#E3E3E9',
  '--app-color-border-default': '#8A8A97',
  '--app-color-border-strong': '#767683',
  '--app-color-interactive-hover': '#F0F0F4',
  '--app-color-interactive-pressed': '#E6E6EC',
  '--app-color-input-bg': 'var(--app-color-surface)',
  '--app-color-disabled-text': '#8A8A97',
  '--app-color-selection-bg':
    'color-mix(in srgb, var(--app-color-accent) 8%, var(--app-color-surface))',
  '--app-color-selection-hover':
    'color-mix(in srgb, var(--app-color-accent) 12%, var(--app-color-surface))',
  '--app-color-nav-active-bg':
    'color-mix(in srgb, var(--app-color-accent) 8%, var(--app-color-surface-sidebar))',
  '--app-color-nav-active-hover':
    'color-mix(in srgb, var(--app-color-accent) 12%, var(--app-color-surface-sidebar))',

  '--app-color-danger-bg': '#B63A3B',
  '--app-color-danger-hover': '#9C2F30',
  '--app-color-danger-pressed': '#842728',
  '--app-color-danger-text': '#B22F31',

  '--app-color-status-success-solid': '#059669',
  '--app-color-status-success-text': '#166534',
  '--app-color-status-success-bg': '#DCFCE7',
  '--app-color-status-success-on-solid': '#0B0F17',
  '--app-color-status-warning-solid': '#CC6F05',
  '--app-color-status-warning-text': '#854D0E',
  '--app-color-status-warning-bg': '#FEF3C7',
  '--app-color-status-warning-on-solid': '#0B0F17',
  '--app-color-status-danger-solid': '#E11D48',
  '--app-color-status-danger-text': '#991B1B',
  '--app-color-status-danger-bg': '#FEE2E2',
  '--app-color-status-danger-on-solid': '#FFFFFF',
  '--app-color-status-info-solid': '#2563EB',
  '--app-color-status-info-text': '#1E40AF',
  '--app-color-status-info-bg': '#DBEAFE',
  '--app-color-status-info-on-solid': '#FFFFFF',
  '--app-color-status-neutral-solid': '#64748B',
  '--app-color-status-neutral-text': '#334155',
  '--app-color-status-neutral-bg': '#E2E8F0',
  '--app-color-status-neutral-on-solid': '#FFFFFF',

  '--app-color-overlay-backdrop': 'rgb(16 16 24 / 0.32)',
  '--app-shadow-popover': '0 1px 2px rgb(16 16 24 / 0.06), 0 8px 24px rgb(16 16 24 / 0.10)',
  '--app-shadow-overlay': '0 2px 4px rgb(16 16 24 / 0.06), 0 16px 48px rgb(16 16 24 / 0.14)',

  '--app-chart-grid': '#E3E3E9',
  '--app-chart-axis': '#62626F',
  '--app-chart-threshold': '#CC6F05',
  '--app-chart-series-1': 'var(--app-color-accent-text)',
  '--app-chart-series-2': '#2563EB',
  '--app-chart-series-3': '#059669',
  '--app-chart-series-4': '#CC6F05',
} as const
