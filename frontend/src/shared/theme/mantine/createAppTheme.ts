/**
 * Mantine theme built from tokens.
 *
 *   const {theme, cssVariablesResolver} = createAppTheme({
 *     extensions: [uptimeExtension],
 *     labels: {close: t('common.close')},
 *   });
 *   <MantineProvider theme={theme} cssVariablesResolver={cssVariablesResolver}>
 *
 * Language: rebuild the theme when the language changes (useMemo keyed by locale).
 */
import type { CSSVariablesResolver, MantineThemeOverride } from '@mantine/core'
import { createBridge } from './bridge'
import { createCssVariablesResolver } from './cssVariables'
import { buildMantineTheme } from './theme'
import type { AppThemeLabels } from './types'
import { createControlVars, createVariantColorResolver } from './variant'
import type { BrandInput } from '../tokens/brand'
import { composeTokens } from '../tokens/compose'
import type { ThemeExtension } from '../tokens/extension'

export interface AppThemeOptions {
  readonly brand?: BrandInput
  readonly extensions?: readonly ThemeExtension[]
  /** UI labels owned by the application (the theme carries no UI text). */
  readonly labels: AppThemeLabels
}

export interface AppTheme {
  readonly theme: MantineThemeOverride
  readonly cssVariablesResolver: CSSVariablesResolver
}

export function createAppTheme(options: AppThemeOptions): AppTheme {
  const { brand, extensions = [], labels } = options
  const composeOptions = brand === undefined ? { extensions } : { brand, extensions }
  const tokens = composeTokens(composeOptions)
  const semantic = tokens.semanticColors
  const variantColorResolver = createVariantColorResolver(semantic, tokens)
  const controlVars = createControlVars(semantic, tokens)
  const bridge = createBridge(semantic, tokens)
  const theme = buildMantineTheme({ tokens, semantic, labels, variantColorResolver, controlVars })
  const cssVariablesResolver = createCssVariablesResolver(tokens, bridge)
  return { theme, cssVariablesResolver }
}
