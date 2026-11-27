import { expect, it } from 'vitest'
import i18n from '../app/i18n'
import { ApiException } from '../shared/api/apiException'
import { ErrorCode, Language } from '../shared/api/generated/enums'
import { LIST_CHECKS_QUERY_Q_MAX_LENGTH } from '../shared/api/generated/limits'
import { validateChecksSearch } from '../features/checks/api/checks.search'
import { settingFieldError } from '../features/settings/settingsModel'
import { SettingFieldClientErrorCode } from '../features/settings/types'

it('the contract error codes have one vocabulary in the client', () => {
  // given the codes the server sends and the codes the client adds
  const contractCodes: readonly string[] = Object.values(ErrorCode)
  const localCodes: readonly string[] = Object.values(SettingFieldClientErrorCode)

  // then the client adds no second name for a contract code
  expect(localCodes.filter((code) => contractCodes.includes(code))).toEqual([])

  // and each field failure keeps the code its source assigns
  const outOfPlan = new ApiException({ code: ErrorCode.OUT_OF_PLAN, params: { bound: 300 } })
  const validation = new ApiException({ code: ErrorCode.VALIDATION_FAILED })
  const serverFailure = new ApiException({ code: ErrorCode.INTERNAL_ERROR })

  expect(settingFieldError(outOfPlan, 's')?.code).toBe(ErrorCode.OUT_OF_PLAN)
  expect(settingFieldError(validation, undefined)?.code).toBe(ErrorCode.VALIDATION_FAILED)
  expect(settingFieldError(serverFailure, undefined)?.code).toBe(
    SettingFieldClientErrorCode.SAVE_FAILED,
  )
})

it('the app languages come from the contract', () => {
  // given the languages generated from the contract
  const languages = Object.values(Language)

  // then the default language is a contract value
  expect(i18n.language).toBe(Language.PL)

  // and the fallback is the contract fallback
  const fallbacks = Array.isArray(i18n.options.fallbackLng)
    ? i18n.options.fallbackLng
    : [i18n.options.fallbackLng]
  expect(fallbacks).toContain(Language.EN)

  // and the shipped bundles are exactly the contract languages
  expect(Object.keys(i18n.options.resources ?? {}).sort()).toEqual([...languages].sort())
})

it('the check search caps the text query at the contract limit', () => {
  // given a query above the contract limit and one exactly at it
  const overLimit = 'a'.repeat(LIST_CHECKS_QUERY_Q_MAX_LENGTH + 20)
  const atLimit = 'a'.repeat(LIST_CHECKS_QUERY_Q_MAX_LENGTH)

  // when the search params are validated
  const capped = validateChecksSearch({ q: overLimit })
  const unchanged = validateChecksSearch({ q: atLimit })

  // then the query is cut to the contract limit and a shorter one is untouched
  expect(capped.q).toBe(overLimit.slice(0, LIST_CHECKS_QUERY_Q_MAX_LENGTH))
  expect(capped.q).toHaveLength(LIST_CHECKS_QUERY_Q_MAX_LENGTH)
  expect(unchanged.q).toBe(atLimit)
})
