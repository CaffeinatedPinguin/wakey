import { defaultCssVariablesResolver, type CSSVariablesResolver } from '@mantine/core'
import type { ComposedTokens } from '../tokens/compose'

export function createCssVariablesResolver(
  tokens: ComposedTokens,
  bridge: Record<string, string>,
): CSSVariablesResolver {
  return (theme) => {
    const base = defaultCssVariablesResolver(theme)
    return {
      variables: { ...base.variables, ...tokens.shared },
      light: { ...base.light, ...tokens.light, ...bridge },
      dark: { ...base.dark, ...tokens.dark, ...bridge },
    }
  }
}
