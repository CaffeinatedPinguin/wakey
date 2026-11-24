import { describe, expect, it } from 'vitest'
import { resolveToken } from './resolve'
import type { TokenMap } from '../tokens/types'

describe('token resolution', () => {
  it('a token reference cycle fails with a clear error', () => {
    const vars: TokenMap = { '--a': 'var(--b)', '--b': 'var(--a)' }
    expect(() => resolveToken(vars, '--a')).toThrow('Too deep token reference')
  })
})
