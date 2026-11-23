import { darkTokens } from './core.dark'
import { lightTokens } from './core.light'
import { sharedTokens } from './core.shared'
import type { SchemeTokens } from './types'

/**
 * Base tokens common to every system. No brand (brand.ts) and no domain
 * (extensions/*). No dependencies, so they also work outside React and
 * Mantine (light status page shell, tokens.css/tokens.json generator).
 */
export const coreTokens: SchemeTokens = {
  shared: sharedTokens,
  light: lightTokens,
  dark: darkTokens,
}

/** Backgrounds on which brand colours must work (worst cases). */
export const brandBackgrounds = {
  light: {
    text: ['#FFFFFF', '#F6F6F8', '#F1F1F4', '#F0F0F4'],
    ring: ['#FFFFFF', '#F6F6F8'],
    selectionBase: '#FFFFFF',
    selectionShares: [8, 12],
  },
  dark: {
    text: ['#121316', '#1B1B1F', '#17171B', '#232328', '#26262D'],
    ring: ['#121316', '#1B1B1F', '#232328'],
    selectionBase: '#1B1B1F',
    selectionShares: [12, 16],
  },
} as const
