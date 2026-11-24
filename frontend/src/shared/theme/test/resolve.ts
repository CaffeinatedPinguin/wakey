import { mix } from '../color'
import type { TokenMap } from '../tokens/types'

const HEX = /^#[0-9a-f]{6}$/i
const VARIABLE = /^var\((--[\w-]+)\)$/
const COLOR_MIX = /^color-mix\(in srgb,\s*(.+?)\s+(\d+(?:\.\d+)?)%,\s*(.+)\)$/

export function resolveToken(
  vars: TokenMap,
  name: `--${string}`,
  base = '#FFFFFF',
  depth = 0,
): string {
  if (depth > 20) {
    throw new Error(`Too deep token reference: ${name}`)
  }
  const value = vars[name]
  if (value === undefined) {
    throw new Error(`Missing token ${name}`)
  }
  return resolveValue(vars, value, base, depth)
}

function resolveVariable(
  vars: TokenMap,
  value: string,
  base: string,
  depth: number,
): string | null {
  const variable = VARIABLE.exec(value)
  if (!variable) {
    return null
  }
  const name = variable[1]
  if (!name) {
    throw new Error(`Invalid token reference: ${value}`)
  }
  return resolveToken(vars, name as `--${string}`, base, depth + 1)
}

function resolveMix(vars: TokenMap, value: string, base: string, depth: number): string | null {
  const colorMix = COLOR_MIX.exec(value)
  if (!colorMix) {
    return null
  }
  const [, first, share, second] = colorMix
  if (!first || !share || !second) {
    throw new Error(`Invalid colour mix: ${value}`)
  }
  const a = resolveValue(vars, first, base, depth + 1)
  const b = resolveValue(vars, second, base, depth + 1)
  return mix(a, b, Number(share))
}

export function resolveValue(vars: TokenMap, raw: string, base: string, depth = 0): string {
  if (depth > 20) {
    throw new Error(`Too deep token reference: ${raw}`)
  }
  const value = raw.trim()
  if (HEX.test(value)) {
    return value.toUpperCase()
  }
  if (value === 'transparent') {
    return base
  }
  const variable = resolveVariable(vars, value, base, depth)
  if (variable !== null) {
    return variable
  }
  const mixed = resolveMix(vars, value, base, depth)
  if (mixed !== null) {
    return mixed
  }
  throw new Error(`Unsupported colour value: ${value}`)
}
