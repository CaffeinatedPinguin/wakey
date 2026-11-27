// Mantine theme from tokens
export { createAppTheme } from './mantine/createAppTheme'
export type { AppTheme, AppThemeOptions } from './mantine/createAppTheme'
export type { AppThemeLabels } from './mantine/types'

// Tokens without dependencies (also for systems without Mantine)
export { composeTokens } from './tokens/compose'
export type { ComposedTokens, ComposeOptions } from './tokens/compose'
export { coreTokens, brandBackgrounds } from './tokens/core'
export { createBrandTokens, DEFAULT_BRAND } from './tokens/brand'
export type { BrandAdjustment, BrandInput, BrandResult } from './tokens/brand'
export type { ThemeExtension } from './tokens/extension'
export type { SchemeTokens, TokenMap } from './tokens/types'
export { rem } from './tokens/types'
export {
  adjustForContrast,
  contrastRatio,
  hexToRgb,
  mix,
  relativeLuminance,
  rgbToHex,
} from './color'

// Extensions
export { uptimeExtension } from './extensions/uptime'
