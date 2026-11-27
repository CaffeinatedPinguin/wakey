import type { CSSProperties } from 'react'

export const touchStyle: CSSProperties = { minHeight: 'var(--app-size-control-touch)' }

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

export const sheetStyles = {
  content: {
    flex: '0 0 auto',
    width: '100%',
    height: 'auto',
    maxHeight: '80dvh',
    borderRadius: 'var(--app-radius-overlay) var(--app-radius-overlay) 0 0',
  },
}
