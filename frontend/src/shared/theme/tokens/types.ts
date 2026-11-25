/** The two colour schemes every token set is composed for. */
export const ColorScheme = { LIGHT: 'light', DARK: 'dark' } as const
export type ColorScheme = (typeof ColorScheme)[keyof typeof ColorScheme]

/** CSS custom property map: name → value. */
export type TokenMap = Readonly<Record<`--${string}`, string>>

export interface SchemeTokens {
  /** Independent of the scheme: sizes, radius, typography, layout, motion. */
  readonly shared: TokenMap
  readonly light: TokenMap
  readonly dark: TokenMap
}

/** px → rem (16 px base), without a Mantine dependency. */
export const rem = (px: number): string => `${(px / 16).toString()}rem`
