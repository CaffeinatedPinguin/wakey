import { defaultVariantColorsResolver, type VariantColorsResolver } from '@mantine/core'
import { relativeLuminance } from '../color'
import type { ComposedTokens } from '../tokens/compose'
import { ColorScheme, type TokenMap } from '../tokens/types'
import { controlBorder, transparentBorder } from './styles'
import type { SemanticMap } from './types'

type ResolverInput = Parameters<VariantColorsResolver>[0]
type Resolved = ReturnType<VariantColorsResolver>
const MixKeyword = { BLACK: 'black', WHITE: 'white' } as const
type MixKeyword = (typeof MixKeyword)[keyof typeof MixKeyword]
export type ControlVars = (
  variant: string | undefined,
  color: string | undefined,
) => Record<string, string>

const VARIABLE = /^var\((--[\w-]+)\)$/
const MAX_ALIAS_DEPTH = 20

const isDanger = (color: string | undefined): boolean => color === 'danger' || color === 'red'

const isAccent = (color: string | undefined): boolean =>
  color === undefined || color === 'brand' || color === 'primary'

function schemeVars(tokens: ComposedTokens, scheme: ColorScheme): TokenMap {
  return { ...tokens.shared, ...tokens[scheme] }
}

function resolveAlias(vars: TokenMap, name: `--${string}`, depth = 0): string {
  if (depth > MAX_ALIAS_DEPTH) {
    throw new Error(`Too deep token reference: ${name}`)
  }
  const value = vars[name]
  if (value === undefined) {
    throw new Error(`Missing token ${name}`)
  }
  const alias = VARIABLE.exec(value)?.[1]
  if (alias === undefined) {
    return value
  }
  return resolveAlias(vars, alias as `--${string}`, depth + 1)
}

/** The text on a solid state: dark text needs a lighter fill, light text a darker one. */
function mixKeyword(vars: TokenMap, prefix: `--${string}`): MixKeyword {
  const text = resolveAlias(vars, `${prefix}-on-solid` as `--${string}`)
  return relativeLuminance(text) < 0.18 ? MixKeyword.WHITE : MixKeyword.BLACK
}

function filledMix(tokens: ComposedTokens, scheme: ColorScheme, prefix: `--${string}`): string {
  const target = mixKeyword(schemeVars(tokens, scheme), prefix)
  return `color-mix(in srgb, var(${prefix}-solid) 88%, ${target})`
}

function pressedMix(tokens: ComposedTokens, scheme: ColorScheme, prefix: `--${string}`): string {
  const target = mixKeyword(schemeVars(tokens, scheme), prefix)
  return `color-mix(in srgb, var(${prefix}-solid) 76%, ${target})`
}

// A state colour can need a different mix direction per scheme (DOWN is white text
// in light and dark text in dark), so emit the CSS light-dark() switch.
function perScheme(light: string, dark: string): string {
  return light === dark ? light : `light-dark(${light}, ${dark})`
}

export function filledHover(tokens: ComposedTokens, prefix: `--${string}`): string {
  return perScheme(
    filledMix(tokens, ColorScheme.LIGHT, prefix),
    filledMix(tokens, ColorScheme.DARK, prefix),
  )
}

export function statePressed(tokens: ComposedTokens, prefix: `--${string}`): string {
  return perScheme(
    pressedMix(tokens, ColorScheme.LIGHT, prefix),
    pressedMix(tokens, ColorScheme.DARK, prefix),
  )
}

function semanticVariant(prefix: `--${string}`, variant: string, tokens: ComposedTokens): Resolved {
  if (variant === 'filled') {
    return {
      background: `var(${prefix}-solid)`,
      hover: filledHover(tokens, prefix),
      color: `var(${prefix}-on-solid)`,
      border: transparentBorder,
    }
  }
  if (variant === 'outline' || variant === 'default') {
    return {
      background: 'transparent',
      hover: `var(${prefix}-bg)`,
      color: `var(${prefix}-text)`,
      border: controlBorder(`var(${prefix}-solid)`),
    }
  }
  if (variant === 'subtle') {
    return {
      background: 'transparent',
      hover: `var(${prefix}-bg)`,
      color: `var(${prefix}-text)`,
      border: transparentBorder,
    }
  }
  return {
    background: `var(${prefix}-bg)`,
    hover: `var(${prefix}-bg)`,
    color: `var(${prefix}-text)`,
    border: transparentBorder,
  }
}

function filledVariant(color: string | undefined): Resolved {
  return isDanger(color)
    ? {
        background: 'var(--app-color-danger-bg)',
        hover: 'var(--app-color-danger-hover)',
        color: 'var(--app-color-on-danger)',
        border: transparentBorder,
      }
    : {
        background: 'var(--app-color-primary-bg)',
        hover: 'var(--app-color-primary-hover)',
        color: 'var(--app-color-on-primary)',
        border: transparentBorder,
      }
}

function defaultVariant(color: string | undefined): Resolved {
  return {
    background: 'var(--app-color-surface)',
    hover: 'var(--app-color-interactive-hover)',
    color: isDanger(color) ? 'var(--app-color-danger-text)' : 'var(--app-color-text-heading)',
    border: controlBorder('var(--app-color-border-default)'),
  }
}

function outlineVariant(color: string | undefined): Resolved {
  return {
    background: 'transparent',
    hover: 'var(--app-color-selection-bg)',
    color: isDanger(color) ? 'var(--app-color-danger-text)' : 'var(--app-color-accent-text)',
    border: controlBorder('var(--app-color-border-default)'),
  }
}

function lightVariant(color: string | undefined): Resolved {
  return isDanger(color)
    ? {
        background: 'var(--app-color-status-danger-bg)',
        hover: 'var(--app-color-status-danger-bg)',
        color: 'var(--app-color-danger-text)',
        border: transparentBorder,
      }
    : {
        background: 'var(--app-color-selection-bg)',
        hover: 'var(--app-color-selection-hover)',
        color: 'var(--app-color-accent-text)',
        border: transparentBorder,
      }
}

function subtleVariant(color: string | undefined): Resolved {
  if (isDanger(color)) {
    return {
      background: 'transparent',
      hover: 'var(--app-color-status-danger-bg)',
      color: 'var(--app-color-danger-text)',
      border: transparentBorder,
    }
  }
  if (isAccent(color)) {
    return {
      background: 'transparent',
      hover: 'var(--app-color-selection-bg)',
      color: 'var(--app-color-accent-text)',
      border: transparentBorder,
    }
  }
  return {
    background: 'transparent',
    hover: 'var(--app-color-interactive-hover)',
    color: 'var(--app-color-text-body)',
    border: transparentBorder,
  }
}

function regularVariant(input: ResolverInput): Resolved {
  switch (input.variant) {
    case 'filled':
      return filledVariant(input.color)
    case 'default':
      return defaultVariant(input.color)
    case 'outline':
      return outlineVariant(input.color)
    case 'light':
      return lightVariant(input.color)
    case 'subtle':
      return subtleVariant(input.color)
    default:
      return defaultVariantColorsResolver(input)
  }
}

export function createVariantColorResolver(
  semantic: SemanticMap,
  tokens: ComposedTokens,
): VariantColorsResolver {
  return (input) => {
    const { color } = input
    const prefix = color === undefined ? undefined : semantic[color]
    if (prefix) {
      return semanticVariant(prefix, input.variant, tokens)
    }
    return regularVariant(input)
  }
}

function filledPressed(color: string | undefined): string {
  return isDanger(color) ? 'var(--app-color-danger-pressed)' : 'var(--app-color-primary-pressed)'
}

function softPressed(color: string | undefined): string {
  return isDanger(color) ? 'var(--app-color-status-danger-bg)' : 'var(--app-color-selection-hover)'
}

function subtlePressed(color: string | undefined): string {
  if (isDanger(color)) {
    return 'var(--app-color-status-danger-bg)'
  }
  return isAccent(color)
    ? 'var(--app-color-selection-hover)'
    : 'var(--app-color-interactive-pressed)'
}

function regularPressed(variant: string, color: string | undefined): string {
  switch (variant) {
    case 'filled':
      return filledPressed(color)
    case 'outline':
    case 'light':
      return softPressed(color)
    case 'subtle':
      return subtlePressed(color)
    default:
      return 'var(--app-color-interactive-pressed)'
  }
}

function pressedColorFor(
  variant: string,
  color: string | undefined,
  semantic: SemanticMap,
  tokens: ComposedTokens,
): string {
  const prefix = color === undefined ? undefined : semantic[color]
  if (prefix) {
    return variant === 'filled' ? statePressed(tokens, prefix) : `var(${prefix}-bg)`
  }
  return regularPressed(variant, color)
}

export function createControlVars(semantic: SemanticMap, tokens: ComposedTokens): ControlVars {
  return (variant, color) => ({
    '--app-control-pressed': pressedColorFor(variant ?? 'filled', color, semantic, tokens),
  })
}
