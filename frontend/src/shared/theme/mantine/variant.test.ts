import { describe, expect, it } from 'vitest'
import { composeTokens, type ComposedTokens } from '../tokens/compose'
import { uptimeExtension } from '../extensions/uptime'
import { filledHover, statePressed } from './variant'
import type { TokenMap } from '../tokens/types'

const ON_SOLID = '--app-state-down-on-solid'

function withAliasCycle(base: ComposedTokens): ComposedTokens {
  const light: TokenMap = {
    ...base.light,
    [ON_SOLID]: 'var(--cycle-alias)',
    '--cycle-alias': `var(${ON_SOLID})`,
  }
  return { ...base, light }
}

describe('variant alias resolution', () => {
  it('a token alias cycle in variant resolution fails with a clear error', () => {
    const tokens = withAliasCycle(composeTokens({ extensions: [uptimeExtension] }))

    expect(() => filledHover(tokens, '--app-state-down')).toThrow('Too deep token reference')
    expect(() => statePressed(tokens, '--app-state-down')).toThrow('Too deep token reference')
  })
})
