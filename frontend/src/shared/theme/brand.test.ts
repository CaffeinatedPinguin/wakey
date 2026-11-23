import { describe, expect, it } from 'vitest'
import { contrastRatio } from './color'
import { composeTokens } from './tokens/compose'
import { DEFAULT_BRAND, type BrandInput } from './tokens/brand'
import { resolveToken } from './test/resolve'
import type { TokenMap } from './tokens/types'

const BRANDS = ['#0EA5E9', '#FACC15', '#16A34A', '#111111', '#E11D48', '#FFFFFF']

const ACCENT_BACKGROUNDS = [
  '--app-color-page',
  '--app-color-surface',
  '--app-color-surface-subtle',
  '--app-color-surface-raised',
  '--app-color-interactive-hover',
  '--app-color-selection-bg',
  '--app-color-selection-hover',
]

function textFailures(brand: BrandInput, scheme: 'light' | 'dark', vars: TokenMap): string[] {
  const failures: string[] = []
  for (const background of ACCENT_BACKGROUNDS) {
    const bg = resolveToken(vars, background as `--${string}`)
    const text = resolveToken(vars, '--app-color-accent-text', bg)
    const ratio = contrastRatio(text, bg)
    if (ratio < 4.5) {
      failures.push(`${brand.accent} ${scheme} accent text on ${background}: ${ratio.toFixed(2)}`)
    }
  }
  return failures
}

function buttonFailures(brand: BrandInput, scheme: 'light' | 'dark', vars: TokenMap): string[] {
  const buttons = [
    '--app-color-primary-bg',
    '--app-color-primary-hover',
    '--app-color-primary-pressed',
  ]
  const failures: string[] = []
  for (const button of buttons) {
    const bg = resolveToken(vars, button as `--${string}`)
    const text = resolveToken(vars, '--app-color-on-primary', bg)
    const ratio = contrastRatio(text, bg)
    if (ratio < 4.5) {
      failures.push(`${brand.accent} ${scheme} primary text on ${button}: ${ratio.toFixed(2)}`)
    }
  }
  return failures
}

function accentFailures(brand: BrandInput): string[] {
  const tokens = composeTokens({ brand })
  return (['light', 'dark'] as const).flatMap((scheme) => {
    const vars: TokenMap = { ...tokens.shared, ...tokens[scheme] }
    return [...textFailures(brand, scheme, vars), ...buttonFailures(brand, scheme, vars)]
  })
}

const DARK_SURFACES = ['--app-color-page', '--app-color-surface', '--app-color-surface-raised']

function darkSurfaceFailures(brand: BrandInput, button: string, background: string): string[] {
  const tokens = composeTokens({ brand })
  const vars: TokenMap = { ...tokens.shared, ...tokens.dark }
  const failures: string[] = []
  for (const surface of DARK_SURFACES) {
    const ratio = contrastRatio(background, resolveToken(vars, surface as `--${string}`))
    if (ratio < 3) {
      failures.push(`${brand.accent} dark ${button} on ${surface}: ${ratio.toFixed(2)}`)
    }
  }
  return failures
}

function darkButtonFailures(brand: BrandInput): string[] {
  const tokens = composeTokens({ brand })
  const vars: TokenMap = { ...tokens.shared, ...tokens.dark }
  const failures: string[] = []
  for (const button of [
    '--app-color-primary-bg',
    '--app-color-primary-hover',
    '--app-color-primary-pressed',
  ]) {
    const background = resolveToken(vars, button as `--${string}`)
    const text = resolveToken(vars, '--app-color-on-primary', background)
    const ratio = contrastRatio(text, background)
    if (ratio < 4.5) {
      failures.push(`${brand.accent} dark text on ${button}: ${ratio.toFixed(2)}`)
    }
    failures.push(...darkSurfaceFailures(brand, button, background))
  }
  return failures
}

describe('brand colours', () => {
  it('foreign brand colours are adjusted to keep AA contrast', () => {
    const failures = BRANDS.flatMap((accent) => accentFailures({ accent }))
    expect(failures).toEqual([])
  })

  it('a dark brand keeps a visible primary button in the dark scheme', () => {
    const failures = BRANDS.flatMap((accent) => darkButtonFailures({ accent }))
    expect(failures).toEqual([])
    const darkBrand = composeTokens({ brand: { accent: '#111111' } })
    expect(darkBrand.brandWarnings.join(' ')).toMatch(/hover or pressed/i)
    expect(darkBrand.brandAdjustments.some((item) => item.warning !== undefined)).toBe(true)
    expect(composeTokens({ brand: DEFAULT_BRAND }).brandWarnings).toEqual([])
  })

  it('a brand close to the DOWN status carries a warning', () => {
    const warnings = composeTokens({ brand: { accent: '#E11D48' } }).brandWarnings
    expect(warnings.join(' ')).toContain('DOWN')
  })
})
