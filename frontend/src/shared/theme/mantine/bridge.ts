import type { ComposedTokens } from '../tokens/compose'
import type { SemanticMap } from './types'
import { filledHover } from './variant'

const baseBridge = {
  '--overlay-bg': 'var(--app-color-overlay-backdrop)',
  '--mantine-color-anchor': 'var(--app-color-accent-text)',
  '--mantine-color-body': 'var(--app-color-page)',
  '--mantine-color-text': 'var(--app-color-text-body)',
  '--mantine-color-dimmed': 'var(--app-color-text-muted)',
  '--mantine-color-placeholder': 'var(--app-color-input-placeholder)',
  '--mantine-color-error': 'var(--app-color-danger-text)',
  '--mantine-color-default': 'var(--app-color-surface)',
  '--mantine-color-default-hover': 'var(--app-color-interactive-hover)',
  '--mantine-color-default-color': 'var(--app-color-text-heading)',
  '--mantine-color-default-border': 'var(--app-color-border-default)',
  '--mantine-color-disabled': 'var(--app-color-disabled-bg)',
  '--mantine-color-disabled-color': 'var(--app-color-disabled-text)',
  '--mantine-color-disabled-border': 'var(--app-color-border-soft)',
  '--mantine-color-brand-filled': 'var(--app-color-primary-bg)',
  '--mantine-color-brand-filled-hover': 'var(--app-color-primary-hover)',
  '--mantine-color-brand-light': 'var(--app-color-selection-bg)',
  '--mantine-color-brand-light-hover': 'var(--app-color-selection-hover)',
  '--mantine-color-brand-light-color': 'var(--app-color-accent-text)',
  '--mantine-color-brand-outline': 'var(--app-color-accent-text)',
  '--mantine-color-brand-text': 'var(--app-color-accent-text)',
  '--mantine-primary-color-filled': 'var(--app-color-primary-bg)',
  '--mantine-primary-color-filled-hover': 'var(--app-color-primary-hover)',
  '--mantine-primary-color-light': 'var(--app-color-selection-bg)',
  '--mantine-primary-color-light-hover': 'var(--app-color-selection-hover)',
  '--mantine-primary-color-light-color': 'var(--app-color-accent-text)',
} as const

export function createBridge(
  semantic: SemanticMap,
  tokens: ComposedTokens,
): Record<string, string> {
  const bridge: Record<string, string> = { ...baseBridge }
  for (const [name, prefix] of Object.entries(semantic)) {
    Object.assign(bridge, {
      [`--mantine-color-${name}-filled`]: `var(${prefix}-solid)`,
      [`--mantine-color-${name}-filled-hover`]: filledHover(tokens, prefix),
      [`--mantine-color-${name}-light`]: `var(${prefix}-bg)`,
      [`--mantine-color-${name}-light-hover`]: `var(${prefix}-bg)`,
      [`--mantine-color-${name}-light-color`]: `var(${prefix}-text)`,
      [`--mantine-color-${name}-outline`]: `var(${prefix}-text)`,
      [`--mantine-color-${name}-outline-hover`]: `var(${prefix}-bg)`,
      [`--mantine-color-${name}-text`]: `var(${prefix}-text)`,
    })
  }
  return bridge
}
