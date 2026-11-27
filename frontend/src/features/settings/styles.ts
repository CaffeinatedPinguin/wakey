import type { CSSProperties } from 'react'

export const ownSourceStyle: CSSProperties = {
  color: 'var(--app-color-accent-text)',
}

export const sourceDotStyle: CSSProperties = {
  flex: '0 0 auto',
  borderRadius: 'var(--app-radius-pill)',
  background: 'var(--app-color-accent-text)',
}

export const mobileRowStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 'var(--app-space-12)',
  width: '100%',
  minHeight: 'var(--app-size-control-touch)',
  paddingBlock: 'var(--app-space-8)',
  borderBottom: 'thin solid var(--app-color-border-soft)',
  textAlign: 'start',
}

export const mobileRowTextStyle: CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--app-space-4)',
  minWidth: 0,
  flex: 1,
}
