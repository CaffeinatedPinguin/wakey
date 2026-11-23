/**
 * Minimal sRGB colour maths (WCAG 2.x). No dependencies: used by the token
 * generator, brand colour checks and contrast tests.
 */

export type Rgb = readonly [number, number, number]

const clamp = (value: number): number => Math.round(Math.min(255, Math.max(0, value)))

const toHexByte = (value: number): string => clamp(value).toString(16).padStart(2, '0')

export function hexToRgb(hex: string): Rgb {
  const digits = hex.replace('#', '')
  const expanded =
    digits.length === 3 ? Array.from(digits, (digit) => digit + digit).join('') : digits
  if (!/^[0-9a-fA-F]{6}$/.test(expanded)) {
    throw new Error(`Invalid hex colour: ${hex}`)
  }
  const red = parseInt(expanded.slice(0, 2), 16)
  const green = parseInt(expanded.slice(2, 4), 16)
  const blue = parseInt(expanded.slice(4, 6), 16)
  return [red, green, blue]
}

export function rgbToHex([red, green, blue]: Rgb): string {
  const value = `#${toHexByte(red)}${toHexByte(green)}${toHexByte(blue)}`
  return value.toUpperCase()
}

/** Equivalent to `color-mix(in srgb, a p%, b)`. */
export function mix(a: string, b: string, weightOfA: number): string {
  const [ar, ag, ab] = hexToRgb(a)
  const [br, bg, bb] = hexToRgb(b)
  const weight = weightOfA / 100
  const red = ar * weight + br * (1 - weight)
  const green = ag * weight + bg * (1 - weight)
  const blue = ab * weight + bb * (1 - weight)
  return rgbToHex([red, green, blue])
}

export function relativeLuminance(hex: string): number {
  const linear = (channel: number): number => {
    const normalized = channel / 255
    return normalized <= 0.04045 ? normalized / 12.92 : ((normalized + 0.055) / 1.055) ** 2.4
  }
  const [red, green, blue] = hexToRgb(hex)
  return 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)
}

export function contrastRatio(a: string, b: string): number {
  const luminanceA = relativeLuminance(a)
  const luminanceB = relativeLuminance(b)
  return (Math.max(luminanceA, luminanceB) + 0.05) / (Math.min(luminanceA, luminanceB) + 0.05)
}

/**
 * Darkens (toward = '#000000') or lightens (toward = '#FFFFFF') the colour by
 * the smallest step that reaches the required contrast against every
 * background. Returns null when it cannot be reached.
 */
export function adjustForContrast(
  color: string,
  backgrounds: readonly string[],
  minRatio: number,
  toward: '#000000' | '#FFFFFF',
): string | null {
  for (let step = 0; step <= 100; step += 2) {
    const candidate = step === 0 ? color.toUpperCase() : mix(toward, color, step)
    const reachesContrast = backgrounds.every((bg) => contrastRatio(candidate, bg) >= minRatio)
    if (reachesContrast) {
      return candidate
    }
  }
  return null
}
