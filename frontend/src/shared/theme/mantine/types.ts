export interface AppThemeLabels {
  /** aria-label of the close button in Modal and Drawer. */
  readonly close: string
}

/** Semantic colour name → token prefix. */
export type SemanticMap = Readonly<Record<string, `--${string}`>>
