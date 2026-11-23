import {
  createTheme,
  rem,
  type MantineThemeOverride,
  type VariantColorsResolver,
} from '@mantine/core'
import { buildColors } from './colors'
import { buildComponents } from './components'
import type { AppThemeLabels, SemanticMap } from './types'
import type { ControlVars } from './variant'
import type { ComposedTokens } from '../tokens/compose'

const baseThemeOptions = {
  autoContrast: false,
  focusRing: 'auto',
  fontFamily: 'var(--app-font-sans)',
  fontFamilyMonospace: 'var(--app-font-mono)',
  headings: {
    fontFamily: 'var(--app-font-sans)',
    fontWeight: '600',
    sizes: {
      h1: {
        fontSize: 'var(--app-type-page-title-size)',
        lineHeight: 'var(--app-type-page-title-line)',
        fontWeight: '600',
      },
      h2: {
        fontSize: 'var(--app-type-section-title-size)',
        lineHeight: 'var(--app-type-section-title-line)',
        fontWeight: '600',
      },
      h3: {
        fontSize: 'var(--app-type-component-title-size)',
        lineHeight: 'var(--app-type-component-title-line)',
        fontWeight: '600',
      },
      h4: { fontSize: rem(14), lineHeight: rem(20), fontWeight: '600' },
      h5: { fontSize: rem(14), lineHeight: rem(20), fontWeight: '600' },
      h6: { fontSize: rem(12), lineHeight: rem(16), fontWeight: '600' },
    },
  },
  fontSizes: { xs: rem(12), sm: rem(13), md: rem(14), lg: rem(16), xl: rem(18) },
  lineHeights: { xs: '1.35', sm: '1.45', md: '1.45', lg: '1.5', xl: '1.45' },
  spacing: { xs: rem(4), sm: rem(8), md: rem(16), lg: rem(24), xl: rem(32) },
  breakpoints: { xs: '36em', sm: '48em', md: '64em', lg: '80em', xl: '96em' },
  black: '#1C1C23',
  white: '#FFFFFF',
  primaryColor: 'brand',
  primaryShade: { light: 6, dark: 6 },
  defaultRadius: 'md',
  radius: { xs: rem(2), sm: rem(4), md: rem(5), lg: rem(6), xl: rem(10) },
  shadows: {
    xs: 'none',
    sm: 'var(--app-shadow-sm)',
    md: 'var(--app-shadow-popover)',
    lg: 'var(--app-shadow-overlay)',
    xl: 'var(--app-shadow-overlay)',
  },
} as const

export interface BuildThemeInput {
  readonly tokens: ComposedTokens
  readonly semantic: SemanticMap
  readonly labels: AppThemeLabels
  readonly variantColorResolver: VariantColorsResolver
  readonly controlVars: ControlVars
}

export function buildMantineTheme(input: BuildThemeInput): MantineThemeOverride {
  return createTheme({
    ...baseThemeOptions,
    colors: buildColors(input.tokens, input.semantic),
    variantColorResolver: input.variantColorResolver,
    components: buildComponents(input.controlVars, input.labels),
  })
}
