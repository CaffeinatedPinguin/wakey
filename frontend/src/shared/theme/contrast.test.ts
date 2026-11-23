import { describe, expect, it } from 'vitest'
import { contrastRatio } from './color'
import { composeTokens } from './tokens/compose'
import { uptimeExtension } from './extensions/uptime'
import { resolveToken } from './test/resolve'
import type { ComposedTokens } from './tokens/compose'
import type { TokenMap } from './tokens/types'

type Check = readonly [fg: string, bg: string, min: number, onBase?: string]

const SURFACES = [
  '--app-color-page',
  '--app-color-surface',
  '--app-color-surface-subtle',
  '--app-color-surface-raised',
  '--app-color-interactive-hover',
]

function textChecks(): Check[] {
  const list: Check[] = []
  for (const fg of [
    '--app-color-text-heading',
    '--app-color-text-body',
    '--app-color-text-muted',
    '--app-color-accent-text',
    '--app-color-danger-text',
  ]) {
    for (const bg of SURFACES) {
      list.push([fg, bg, 4.5])
    }
  }
  for (const bg of ['--app-color-selection-bg', '--app-color-selection-hover']) {
    list.push(['--app-color-accent-text', bg, 4.5], ['--app-color-text-heading', bg, 4.5])
  }
  return list
}

function controlChecks(): Check[] {
  const list: Check[] = [
    [
      '--app-color-nav-active-text',
      '--app-color-nav-active-bg',
      4.5,
      '--app-color-surface-sidebar',
    ],
    [
      '--app-color-nav-active-indicator',
      '--app-color-nav-active-bg',
      3,
      '--app-color-surface-sidebar',
    ],
    ['--app-color-input-placeholder', '--app-color-input-bg', 4.5],
    ['--app-color-danger-text', '--app-color-status-danger-bg', 4.5],
  ]
  for (const bg of [
    '--app-color-primary-bg',
    '--app-color-primary-hover',
    '--app-color-primary-pressed',
  ]) {
    list.push(['--app-color-on-primary', bg, 4.5])
  }
  for (const bg of [
    '--app-color-danger-bg',
    '--app-color-danger-hover',
    '--app-color-danger-pressed',
  ]) {
    list.push(['--app-color-on-danger', bg, 4.5])
  }
  for (const bg of ['--app-color-input-bg', '--app-color-surface', '--app-color-surface-raised']) {
    list.push(['--app-color-border-control', bg, 3])
  }
  for (const bg of ['--app-color-page', '--app-color-surface', '--app-color-surface-raised']) {
    list.push(['--app-color-focus-ring', bg, 3])
  }
  return list
}

function statusChecks(): Check[] {
  const list: Check[] = []
  for (const status of ['success', 'warning', 'danger', 'info', 'neutral']) {
    const prefix = `--app-color-status-${status}`
    list.push(
      [`${prefix}-text`, `${prefix}-bg`, 4.5],
      [`${prefix}-on-solid`, `${prefix}-solid`, 4.5],
    )
    for (const bg of ['--app-color-page', '--app-color-surface', '--app-color-surface-raised']) {
      list.push([`${prefix}-solid`, bg, 3])
    }
    list.push([`${prefix}-text`, '--app-color-surface', 4.5])
  }
  for (const series of [1, 2, 3, 4]) {
    list.push([`--app-chart-series-${String(series)}`, '--app-color-surface', 3])
  }
  return list
}

function extensionChecks(vars: TokenMap): Check[] {
  const list: Check[] = []
  if ('--app-color-admin-accent' in vars) {
    list.push([
      '--app-color-admin-accent',
      '--app-color-admin-bg',
      4.5,
      '--app-color-surface-sidebar',
    ])
  }
  if ('--app-state-nodata-border' in vars) {
    list.push(['--app-state-nodata-border', '--app-color-surface', 3])
  }
  if ('--app-state-marker' in vars) {
    list.push(['--app-state-marker', '--app-color-surface', 3])
  }
  return list
}

function checks(vars: TokenMap): Check[] {
  return [...textChecks(), ...controlChecks(), ...statusChecks(), ...extensionChecks(vars)]
}

function failuresFor(tokens: ComposedTokens, scheme: 'light' | 'dark'): string[] {
  const vars: TokenMap = { ...tokens.shared, ...tokens[scheme] }
  const failures: string[] = []
  for (const [fg, bg, min, onBase] of checks(vars)) {
    const base = onBase
      ? resolveToken(vars, onBase as `--${string}`)
      : resolveToken(vars, '--app-color-surface')
    const foreground = resolveToken(vars, fg as `--${string}`, base)
    const background = resolveToken(vars, bg as `--${string}`, base)
    const ratio = contrastRatio(foreground, background)
    if (ratio < min) {
      failures.push(`${scheme} ${fg} on ${bg}: ${ratio.toFixed(2)} < ${String(min)}`)
    }
  }
  return failures
}

describe('theme contrast', () => {
  it('every text and control pair meets WCAG AA in light and dark', () => {
    const themes = [composeTokens({ extensions: [uptimeExtension] }), composeTokens()]
    const failures = themes.flatMap((tokens) =>
      (['light', 'dark'] as const).flatMap((scheme) => failuresFor(tokens, scheme)),
    )
    expect(failures).toEqual([])
  })
})
