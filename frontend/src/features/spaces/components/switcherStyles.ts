import type { CSSProperties } from 'react'
import type { SpaceSummary } from '../spaceTypes'

export const touchStyle: CSSProperties = { minHeight: 'var(--app-size-control-touch)' }

export const sheetStyles = {
  content: {
    flex: '0 0 auto',
    width: '100%',
    height: 'auto',
    maxHeight: '80dvh',
    borderRadius: 'var(--app-radius-overlay) var(--app-radius-overlay) 0 0',
  },
}

export const sheetRowStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 'var(--app-space-control-inline)',
  width: '100%',
  minHeight: 'var(--app-size-control-touch)',
  paddingInline: 'var(--app-space-sidebar-inline)',
  borderRadius: 'var(--app-radius-control)',
  color: 'var(--app-color-text-body)',
}

export const avatarStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  width: 'var(--app-size-control-compact)',
  height: 'var(--app-size-control-compact)',
  flex: '0 0 auto',
  borderRadius: 'var(--app-radius-control)',
  background: 'var(--app-color-primary-bg)',
  color: 'var(--app-color-on-primary)',
  fontWeight: 600,
}

export function currentName(
  spaces: readonly SpaceSummary[],
  currentSlug: string,
): string | undefined {
  return spaces.find((space) => space.slug === currentSlug)?.name
}
