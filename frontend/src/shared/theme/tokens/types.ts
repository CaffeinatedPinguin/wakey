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
