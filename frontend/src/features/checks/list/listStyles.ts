import type { CSSProperties } from 'react'

export const cardStyle: CSSProperties = {
  minHeight: 'var(--app-size-control-touch)',
  borderRadius: 'var(--app-radius-card)',
  background: 'var(--app-color-surface)',
}

export const monoStyle: CSSProperties = { fontFamily: 'var(--app-font-mono)' }

export const selectionBarStyle: CSSProperties = {
  background: 'var(--app-color-selection-bg)',
  borderColor: 'var(--app-color-accent-text)',
}
