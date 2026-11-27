import { describe, expect, it } from 'vitest'
import {
  CheckState,
  CheckType,
  ErrorCategory,
  PausedReason,
  SettingSource,
} from '../../../shared/api/generated/enums'
import { settingSourceSchema } from '../../settings/api/settings.schemas'
import {
  checkStateSchema,
  checkTypeSchema,
  errorCategorySchema,
  pausedReasonSchema,
} from './checks.schemas'

interface ClosedSetSchema {
  options: readonly string[]
  safeParse: (value: unknown) => { success: boolean }
}

function expectExactContractValues(schema: ClosedSetSchema, values: readonly string[]): void {
  expect([...schema.options].sort()).toEqual([...values].sort())
  for (const value of values) {
    expect(schema.safeParse(value).success).toBe(true)
  }
  expect(schema.safeParse('not_a_contract_value').success).toBe(false)
}

describe('closed sets of the client', () => {
  it('every closed set of the client is derived from the contract', () => {
    // given the closed sets generated from the API contract
    // when the schemas validate their values
    // then each schema accepts exactly the contract values and nothing else
    expectExactContractValues(checkStateSchema, Object.values(CheckState))
    expectExactContractValues(checkTypeSchema, Object.values(CheckType))
    expectExactContractValues(pausedReasonSchema, Object.values(PausedReason))
    expectExactContractValues(errorCategorySchema, Object.values(ErrorCategory))
    expectExactContractValues(settingSourceSchema, Object.values(SettingSource))
  })
})
