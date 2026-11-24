import { describe, expect, it } from 'vitest'
import { contrastRatio, mix, relativeLuminance } from './color'
import { composeTokens } from './tokens/compose'
import { uptimeExtension } from './extensions/uptime'
import { resolveToken } from './test/resolve'
import type { TokenMap } from './tokens/types'

const SEMANTIC_COLOURS = {
  up: '--app-state-up',
  degraded: '--app-state-degraded',
  down: '--app-state-down',
  unknown: '--app-state-unknown',
  maintenance: '--app-state-maintenance',
  success: '--app-color-status-success',
  warning: '--app-color-status-warning',
  info: '--app-color-status-info',
  neutral: '--app-color-status-neutral',
} as const

function targetFor(onSolid: string): string {
  return relativeLuminance(onSolid) < 0.18 ? '#FFFFFF' : '#000000'
}

function filledColour(vars: TokenMap, prefix: `--${string}`, share: number): string {
  const solid = resolveToken(vars, `${prefix}-solid` as `--${string}`)
  const onSolid = resolveToken(vars, `${prefix}-on-solid` as `--${string}`, solid)
  return mix(solid, targetFor(onSolid), share)
}

function failuresFor(vars: TokenMap, name: string, prefix: `--${string}`): string[] {
  const solid = resolveToken(vars, `${prefix}-solid` as `--${string}`)
  const onSolid = resolveToken(vars, `${prefix}-on-solid` as `--${string}`, solid)
  const hover = filledColour(vars, prefix, 88)
  const pressed = filledColour(vars, prefix, 76)
  const failures: string[] = []
  if (hover === solid) {
    failures.push(`${name}: hover equals solid`)
  }
  if (pressed === solid) {
    failures.push(`${name}: pressed equals solid`)
  }
  if (contrastRatio(onSolid, hover) < 4.5) {
    failures.push(`${name}: hover contrast ${contrastRatio(onSolid, hover).toFixed(2)}`)
  }
  if (contrastRatio(onSolid, pressed) < 4.5) {
    failures.push(`${name}: pressed contrast ${contrastRatio(onSolid, pressed).toFixed(2)}`)
  }
  return failures
}

describe('filled state colours', () => {
  it('filled state colours have readable hover and pressed states', () => {
    const tokens = composeTokens({ extensions: [uptimeExtension] })
    const failures = (['light', 'dark'] as const).flatMap((scheme) => {
      const vars: TokenMap = { ...tokens.shared, ...tokens[scheme] }
      return Object.entries(SEMANTIC_COLOURS).flatMap(([name, prefix]) =>
        failuresFor(vars, name, prefix),
      )
    })
    expect(failures).toEqual([])
  })
})
