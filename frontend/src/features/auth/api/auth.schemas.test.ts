import { describe, expect, it } from 'vitest'
import type { GetMeResponse } from '../../../shared/api/generated/types'
import { meSchema } from './auth.schemas'
import type { Me } from './auth.schemas'

describe('me schema', () => {
  it('the me response is parsed against the contract type', () => {
    const response: GetMeResponse = { subject: 'alice', admin: true }

    const parsed: Me = meSchema.parse(response)

    expect(parsed).toEqual({ subject: 'alice', admin: true })
    expect(() => meSchema.parse({ subject: 'alice' })).toThrow()
  })
})
