import type { MantineColorsTuple } from '@mantine/core'
import { mix } from '../color'
import type { ComposedTokens } from '../tokens/compose'
import type { SemanticMap } from './types'

const tuple = (...colors: string[]): MantineColorsTuple => colors as unknown as MantineColorsTuple

const grayTuple = tuple(
  '#f6f6f8',
  '#f1f1f4',
  '#e3e3e9',
  '#c9c9d3',
  '#a2a2ae',
  '#8a8a97',
  '#62626f',
  '#3d3d48',
  '#232328',
  '#1b1b1f',
)

const dangerTuple = tuple(
  '#ffefef',
  '#ffdedf',
  '#f9b8b9',
  '#f28f90',
  '#ed7172',
  '#eb5f60',
  '#ea5455',
  '#d04445',
  '#b63a3b',
  '#9c2f30',
)

/** Tuples only for Mantine internals; components get colours from tokens. */
const statusTuples: Record<string, MantineColorsTuple> = {
  '--app-color-status-success': tuple(
    '#dcfce7',
    '#bbf7d0',
    '#86efac',
    '#4ade80',
    '#22c55e',
    '#10b981',
    '#059669',
    '#047857',
    '#166534',
    '#14532d',
  ),
  '--app-color-status-warning': tuple(
    '#fef3c7',
    '#fde68a',
    '#fcd34d',
    '#fbbf24',
    '#f59e0b',
    '#e18a05',
    '#cc6f05',
    '#b45309',
    '#854d0e',
    '#713f12',
  ),
  '--app-color-status-danger': tuple(
    '#fee2e2',
    '#fecaca',
    '#fca5a5',
    '#fb7185',
    '#f43f5e',
    '#e11d48',
    '#be123c',
    '#9f1239',
    '#991b1b',
    '#7f1d1d',
  ),
  '--app-color-status-info': tuple(
    '#eff6ff',
    '#dbeafe',
    '#bfdbfe',
    '#93c5fd',
    '#60a5fa',
    '#3b82f6',
    '#2563eb',
    '#1d4ed8',
    '#1e40af',
    '#1e3a8a',
  ),
  '--app-color-status-neutral': tuple(
    '#f1f5f9',
    '#e2e8f0',
    '#cbd5e1',
    '#94a3b8',
    '#7b8aa0',
    '#64748b',
    '#475569',
    '#334155',
    '#1e293b',
    '#0f172a',
  ),
}

/** Resolves a `var(--x-solid)` alias to the base status tuple when an extension points at core. */
function tupleFor(prefix: string, light: ComposedTokens['light']): MantineColorsTuple {
  const known = statusTuples[prefix]
  if (known) {
    return known
  }
  const solid = light[`${prefix}-solid` as `--${string}`]
  const alias = solid?.match(/var\((--app-color-status-[a-z]+)-solid\)/)?.[1]
  if (alias) {
    const fromAlias = statusTuples[alias]
    if (fromAlias) {
      return fromAlias
    }
  }
  return grayTuple
}

function brandTuple(accent: string): MantineColorsTuple {
  return tuple(
    mix(accent, '#FFFFFF', 8),
    mix(accent, '#FFFFFF', 16),
    mix(accent, '#FFFFFF', 30),
    mix(accent, '#FFFFFF', 46),
    mix(accent, '#FFFFFF', 70),
    mix(accent, '#FFFFFF', 85),
    accent,
    mix('#000000', accent, 10),
    mix('#000000', accent, 20),
    mix('#000000', accent, 32),
  )
}

export function buildColors(
  tokens: ComposedTokens,
  semantic: SemanticMap,
): Record<string, MantineColorsTuple> {
  const accent = tokens.light['--app-color-accent'] ?? '#7367F0'
  const colors: Record<string, MantineColorsTuple> = {
    brand: brandTuple(accent),
    gray: grayTuple,
    danger: dangerTuple,
  }
  for (const [name, prefix] of Object.entries(semantic)) {
    colors[name] = tupleFor(prefix, tokens.light)
  }
  return colors
}
