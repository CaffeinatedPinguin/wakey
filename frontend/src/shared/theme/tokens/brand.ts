/**
 * Brand colour → accent tokens with guaranteed contrast.
 *
 * The default brand (#7367F0) returns hand-tuned values. Any other colour
 * (another system or a status page colour) is adjusted automatically:
 * - accent text ≥ 4.5:1 on every surface,
 * - white text on the primary button ≥ 4.5:1,
 * - focus ring ≥ 3:1.
 *
 * The report lists every change so the UI can show a warning instead of
 * guessing. The brand never changes status or state colours.
 */
import { adjustForContrast, contrastRatio, hexToRgb, mix } from '../color'
import { brandBackgrounds } from './core'
import type { TokenMap } from './types'

export interface BrandInput {
  /** Brand colour in hex, e.g. '#7367F0'. */
  readonly accent: string
}

export interface BrandAdjustment {
  readonly token: string
  readonly scheme: 'light' | 'dark'
  readonly from: string
  readonly to: string
  readonly reason: string
}

export interface BrandResult {
  readonly light: TokenMap
  readonly dark: TokenMap
  readonly adjustments: readonly BrandAdjustment[]
  /** Warnings that cannot be fixed automatically (e.g. a colour close to a state). */
  readonly warnings: readonly string[]
}

/** Status hues (HSL). A brand too close to one of them is confused with a state. */
const STATUS_HUES = { success: 160, warning: 38, danger: 350, info: 217 } as const

/** User-facing state names for the brand warning. */
const STATUS_LABELS: Readonly<Record<keyof typeof STATUS_HUES, string>> = {
  success: 'UP',
  warning: 'DEGRADED',
  danger: 'DOWN',
  info: 'MAINTENANCE',
}

export const DEFAULT_BRAND: BrandInput = { accent: '#7367F0' }

const DEFAULT_BRAND_TOKENS: BrandResult = {
  light: {
    '--app-color-accent': '#7367F0',
    '--app-color-accent-text': '#5B4FD3',
    '--app-color-primary-bg': '#665AD8',
    '--app-color-primary-hover': '#5A4FC0',
    '--app-color-primary-pressed': '#4D449F',
    '--app-color-on-primary': '#FFFFFF',
  },
  dark: {
    '--app-color-accent': '#7367F0',
    '--app-color-accent-text': '#8E84F5',
    '--app-color-primary-bg': '#665AD8',
    '--app-color-primary-hover': '#5A4FC0',
    '--app-color-primary-pressed': '#4D449F',
    '--app-color-on-primary': '#FFFFFF',
  },
  adjustments: [],
  warnings: [],
}

interface HueAndSaturation {
  readonly h: number
  readonly s: number
}

interface RgbChannels {
  readonly red: number
  readonly green: number
  readonly blue: number
}

function normalizedChannels(hex: string): RgbChannels {
  const [red, green, blue] = hexToRgb(hex)
  return { red: red / 255, green: green / 255, blue: blue / 255 }
}

function hueFor(channels: RgbChannels, max: number, delta: number): number {
  if (max === channels.red) {
    return ((channels.green - channels.blue) / delta) % 6
  }
  if (max === channels.green) {
    return (channels.blue - channels.red) / delta + 2
  }
  return (channels.red - channels.green) / delta + 4
}

function hueAndSaturation(hex: string): HueAndSaturation {
  const channels = normalizedChannels(hex)
  const { red, green, blue } = channels
  const max = Math.max(red, green, blue)
  const min = Math.min(red, green, blue)
  const delta = max - min
  const lightness = (max + min) / 2
  if (delta === 0) {
    return { h: 0, s: 0 }
  }
  const saturation = delta / (1 - Math.abs(2 * lightness - 1))
  const hue = hueFor(channels, max, delta)
  return { h: (hue * 60 + 360) % 360, s: saturation }
}

function statusConflicts(accent: string): string[] {
  const { h, s } = hueAndSaturation(accent)
  if (s < 0.35) {
    return []
  }
  const hues = Object.entries(STATUS_HUES) as [keyof typeof STATUS_HUES, number][]
  const conflicts = hues.filter(
    ([, hue]) => Math.min(Math.abs(h - hue), 360 - Math.abs(h - hue)) < 20,
  )
  return conflicts.map(
    ([name]) =>
      `The brand colour is close to the ${STATUS_LABELS[name]} status colour. States still use an icon and text, but consider another hue.`,
  )
}

interface SchemeContext {
  readonly scheme: 'light' | 'dark'
  readonly accent: string
  readonly adjustments: BrandAdjustment[]
}

function pickToken(
  context: SchemeContext,
  token: string,
  wanted: string,
  got: string | null,
): string {
  const { scheme, adjustments } = context
  const value = got ?? (scheme === 'light' ? '#000000' : '#FFFFFF')
  if (value !== wanted.toUpperCase()) {
    adjustments.push({ token, scheme, from: wanted, to: value, reason: 'contrast below target' })
  }
  return value
}

function primaryTokens(accent: string, adjustments: BrandAdjustment[]): TokenMap {
  const primaryBg = adjustForContrast(accent, ['#FFFFFF'], 4.5, '#000000') ?? '#000000'
  if (primaryBg !== accent) {
    adjustments.push({
      token: '--app-color-primary-bg',
      scheme: 'light',
      from: accent,
      to: primaryBg,
      reason: 'white text on the primary button < 4.5:1',
    })
  }
  return {
    '--app-color-primary-bg': primaryBg,
    '--app-color-primary-hover': mix('#000000', primaryBg, 12),
    '--app-color-primary-pressed': mix('#000000', primaryBg, 24),
    '--app-color-on-primary': '#FFFFFF',
  }
}

function schemeTokens(
  scheme: 'light' | 'dark',
  accent: string,
  primary: TokenMap,
  adjustments: BrandAdjustment[],
): TokenMap {
  const backgrounds = brandBackgrounds[scheme]
  const toward = scheme === 'light' ? '#000000' : '#FFFFFF'
  const context: SchemeContext = { scheme, accent, adjustments }
  const ring = pickToken(
    context,
    '--app-color-accent',
    accent,
    adjustForContrast(accent, backgrounds.ring, 3, toward),
  )
  const selection = backgrounds.selectionShares.map((share) =>
    mix(ring, backgrounds.selectionBase, share),
  )
  const text = pickToken(
    context,
    '--app-color-accent-text',
    accent,
    adjustForContrast(accent, [...backgrounds.text, ...selection], 4.5, toward),
  )
  return { '--app-color-accent': ring, '--app-color-accent-text': text, ...primary }
}

export function createBrandTokens(input: BrandInput = DEFAULT_BRAND): BrandResult {
  hexToRgb(input.accent) // validates the format
  const accent = input.accent.toUpperCase()
  if (accent === DEFAULT_BRAND.accent.toUpperCase()) {
    return DEFAULT_BRAND_TOKENS
  }
  const adjustments: BrandAdjustment[] = []
  const primary = primaryTokens(accent, adjustments)
  const light = schemeTokens('light', accent, primary, adjustments)
  const dark = schemeTokens('dark', accent, primary, adjustments)
  return { light, dark, adjustments, warnings: statusConflicts(accent) }
}

/** Quick check for an appearance form: does the colour need adjustments? */
export function describeBrand(accent: string): {
  ok: boolean
  result: BrandResult
  contrastOnWhite: number
} {
  const result = createBrandTokens({ accent })
  const ok = result.adjustments.length === 0 && result.warnings.length === 0
  return { ok, result, contrastOnWhite: contrastRatio(accent, '#FFFFFF') }
}
