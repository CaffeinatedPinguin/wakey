import { coreTokens } from './core'
import { type BrandAdjustment, type BrandInput, createBrandTokens } from './brand'
import type { ThemeExtension } from './extension'
import type { TokenMap } from './types'

export interface ComposeOptions {
  readonly brand?: BrandInput
  readonly extensions?: readonly ThemeExtension[]
}

export interface ComposedTokens {
  readonly shared: TokenMap
  readonly light: TokenMap
  readonly dark: TokenMap
  readonly semanticColors: Readonly<Record<string, `--${string}`>>
  readonly brandAdjustments: readonly BrandAdjustment[]
  readonly brandWarnings: readonly string[]
}

/** Core semantic colours available in every system. `danger` = destructive action, not a status. */
const coreSemanticColors = {
  success: '--app-color-status-success',
  warning: '--app-color-status-warning',
  info: '--app-color-status-info',
  neutral: '--app-color-status-neutral',
} as const

const PROTECTED_PREFIXES = ['--app-color-status-', '--app-state-']

interface ComposeState {
  shared: TokenMap
  light: TokenMap
  dark: TokenMap
  semanticColors: Record<string, `--${string}`>
}

function createMerge(taken: ReadonlySet<string>) {
  return (
    base: TokenMap,
    add: TokenMap | undefined,
    extensionName: string,
    seen: Set<string>,
  ): TokenMap => {
    for (const key of Object.keys(add ?? {})) {
      if (taken.has(key)) {
        throw new Error(`Extension "${extensionName}" overrides base token ${key}`)
      }
      if (seen.has(key)) {
        throw new Error(`Extension "${extensionName}" defines ${key} twice`)
      }
      seen.add(key)
    }
    return { ...base, ...add }
  }
}

function addSemanticColors(
  extension: ThemeExtension,
  current: Record<string, `--${string}`>,
): Record<string, `--${string}`> {
  let next = current
  for (const [name, prefix] of Object.entries(extension.semanticColors ?? {})) {
    const reserved = name in next || name === 'danger' || name === 'brand'
    if (reserved) {
      throw new Error(`Extension "${extension.name}" redefines semantic "${name}"`)
    }
    next = { ...next, [name]: prefix }
  }
  return next
}

function assertBrandDoesNotTouchStates(brandTokens: ReturnType<typeof createBrandTokens>): void {
  for (const key of Object.keys(brandTokens.light)) {
    if (PROTECTED_PREFIXES.some((prefix) => key.startsWith(prefix))) {
      throw new Error(`Brand cannot change state token ${key}`)
    }
  }
}

/**
 * Composes tokens: core → brand → extensions. An extension cannot override a
 * core or brand token (it throws), so one system never silently changes the
 * shared theme.
 */
export function composeTokens({ brand, extensions = [] }: ComposeOptions = {}): ComposedTokens {
  const brandTokens = createBrandTokens(brand)
  const taken = new Set<string>([
    ...Object.keys(coreTokens.shared),
    ...Object.keys(coreTokens.light),
    ...Object.keys(coreTokens.dark),
    ...Object.keys(brandTokens.light),
  ])
  const merge = createMerge(taken)
  const seen = {
    shared: new Set<string>(),
    light: new Set<string>(),
    dark: new Set<string>(),
  }
  const state: ComposeState = {
    shared: coreTokens.shared,
    light: { ...coreTokens.light, ...brandTokens.light },
    dark: { ...coreTokens.dark, ...brandTokens.dark },
    semanticColors: { ...coreSemanticColors },
  }
  for (const extension of extensions) {
    state.shared = merge(state.shared, extension.shared, extension.name, seen.shared)
    state.light = merge(state.light, extension.light, extension.name, seen.light)
    state.dark = merge(state.dark, extension.dark, extension.name, seen.dark)
    state.semanticColors = addSemanticColors(extension, state.semanticColors)
  }
  assertBrandDoesNotTouchStates(brandTokens)
  return {
    ...state,
    brandAdjustments: brandTokens.adjustments,
    brandWarnings: brandTokens.warnings,
  }
}
