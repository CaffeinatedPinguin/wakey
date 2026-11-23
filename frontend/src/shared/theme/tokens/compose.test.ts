import { describe, expect, it } from 'vitest'
import { composeTokens } from './compose'
import type { ThemeExtension } from './extension'

const coreCollision: ThemeExtension = {
  name: 'override-core',
  light: { '--app-color-surface': '#000000' },
}

const brandCollision: ThemeExtension = {
  name: 'override-brand',
  light: { '--app-color-accent': '#000000' },
}

const firstExtension: ThemeExtension = {
  name: 'first',
  light: { '--app-state-custom': 'var(--app-color-text-heading)' },
}

const secondExtension: ThemeExtension = {
  name: 'second',
  light: { '--app-state-custom': 'var(--app-color-text-body)' },
}

describe('token composition', () => {
  it('an extension cannot override a core or brand token', () => {
    expect(() => composeTokens({ extensions: [coreCollision] })).toThrow()
    expect(() => composeTokens({ extensions: [brandCollision] })).toThrow()
    expect(() => composeTokens({ extensions: [firstExtension, secondExtension] })).toThrow()
  })
})
