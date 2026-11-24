import { rem } from '@mantine/core'

export const transparentBorder = `${rem(1)} solid transparent`

export const controlBorder = (color: string): string => `${rem(1)} solid ${color}`

// Mantine resolves --input-height-* on the wrapper, so the theme must set them
// there; the input only inherits the resolved values.
export const inputWrapperStyles = {
  '--input-height-xs': 'var(--app-size-control-compact)',
  '--input-height-sm': 'var(--app-size-control-desktop)',
  '--input-height-md': 'var(--app-size-control-desktop)',
  '--input-height-lg': 'var(--app-size-control-large)',
  '--input-height-xl': 'var(--app-size-control-touch)',
} as const

export const inputStyles = {
  fontSize: 'var(--app-type-body-size)',
  '--input-bg': 'var(--app-color-input-bg)',
  '--input-bd': 'var(--app-color-input-border)',
  '--input-bd-focus': 'var(--app-color-focus-ring)',
  '--input-color': 'var(--app-color-text-heading)',
  '--input-placeholder-color': 'var(--app-color-input-placeholder)',
  '--input-radius': 'var(--app-radius-control)',
} as const

export const inputSectionStyles = { color: 'var(--app-color-text-muted)' } as const

export const overlayTransition = {
  duration: 180,
  exitDuration: 180,
  transition: 'fade' as const,
  timingFunction: 'ease-out',
}

export const overlayStyles = {
  content: { background: 'var(--app-color-surface-raised)' },
  header: {
    background: 'var(--app-color-surface-raised)',
    borderBottom: '1px solid var(--app-color-border-soft)',
    minHeight: 'var(--app-size-control-touch)',
    padding: 'var(--app-space-12) var(--app-space-overlay-inset)',
  },
  title: {
    fontSize: 'var(--app-type-overlay-title-size)',
    lineHeight: 'var(--app-type-overlay-title-line)',
    fontWeight: 'var(--app-type-heading-weight)',
    color: 'var(--app-color-text-heading)',
  },
  body: { padding: 'var(--app-space-overlay-inset)' },
} as const

export const dropdownStyles = {
  minWidth: 'min(var(--app-size-dropdown-min), var(--app-size-overlay-available))',
  maxWidth: 'var(--app-size-overlay-available)',
  padding: 'var(--app-space-4)',
  background: 'var(--app-color-surface-raised)',
  borderColor: 'var(--app-color-border-soft)',
  borderRadius: 'var(--app-radius-card)',
  boxShadow: 'var(--app-shadow-popover)',
} as const

export const optionStyles = {
  minHeight: 'var(--app-size-menu-item)',
  fontSize: 'var(--app-type-body-size)',
  lineHeight: 'var(--app-type-body-line)',
  borderRadius: 'var(--app-radius-control)',
  whiteSpace: 'normal',
  overflowWrap: 'anywhere',
} as const
