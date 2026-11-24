import type { TokenMap } from './types'

/**
 * A domain extension of the theme (e.g. uptime monitor states). It has no
 * Mantine dependency so the tokens.css generator and light shells can read it.
 */
export interface ThemeExtension {
  readonly name: string
  readonly shared?: TokenMap
  readonly light?: TokenMap
  readonly dark?: TokenMap
  /**
   * Semantic colours handled by `color="…"` in Mantine components.
   * The value is a token prefix: `${prefix}-solid`, `-text`, `-bg`, `-on-solid`.
   */
  readonly semanticColors?: Readonly<Record<string, `--${string}`>>
}
