import type { CSSProperties } from 'react'

/** Minimum touch target on mobile (44 from the theme). */
export const touchStyle: CSSProperties = { minHeight: 'var(--app-size-control-touch)' }

export const targetStyle: CSSProperties = {
  fontFamily: 'var(--app-font-mono)',
  overflowWrap: 'anywhere',
}
