/** Shared role aliases: they depend on values from a concrete scheme. */
export const roleAliases = {
  '--app-surface-item': 'var(--app-color-surface)',
  '--app-color-selection-indicator': 'var(--app-color-accent-text)',
  '--app-color-focus-ring': 'var(--app-color-accent)',
  '--app-color-input-bg-hover': 'var(--app-color-input-bg)',
  // Control outline = border-default (≥ 3:1, WCAG 1.4.11) in both schemes.
  '--app-color-border-control': 'var(--app-color-border-default)',
  '--app-color-input-border': 'var(--app-color-border-control)',
  '--app-color-input-border-hover':
    'color-mix(in srgb, var(--app-color-input-border) 86%, var(--app-color-text-heading))',
  '--app-color-input-placeholder': 'var(--app-color-text-muted)',
  '--app-color-disabled-bg': 'var(--app-color-surface-subtle)',
  '--app-color-nav-active-text': 'var(--app-color-accent-text)',
  '--app-color-nav-active-icon': 'var(--app-color-accent-text)',
  '--app-color-nav-active-indicator': 'var(--app-color-accent-text)',
  '--app-color-on-danger': '#FFFFFF',
  '--app-shadow-sm': '0 1px 2px rgb(0 0 0 / 0.06)',
} as const
