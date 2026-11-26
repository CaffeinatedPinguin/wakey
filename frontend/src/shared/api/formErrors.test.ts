import { useForm } from '@mantine/form'
import { act, cleanup, renderHook } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '../../app/i18n'
import { commonResources } from '../i18n/resources'
import { ApiException, ClientErrorCode } from './apiException'
import { applyApiException } from './formErrors'
import { ErrorCode } from './generated/enums'

interface TestFormValues {
  name: string
  slug: string
}

const pl = i18n.getFixedT('pl')

function renderForm() {
  return renderHook(() => useForm<TestFormValues>({ initialValues: { name: '', slug: '' } }))
}

afterEach(cleanup)

describe('server error translations', () => {
  it('every error code has a translation in both languages', () => {
    for (const code of Object.values(ErrorCode)) {
      const key = `errors.${code}`
      const translationPl = commonResources.pl.errors[code]
      const translationEn = commonResources.en.errors[code]
      expect(translationPl).toBeDefined()
      expect(translationEn).toBeDefined()
      expect(translationPl).not.toBe(key)
      expect(translationEn).not.toBe(key)
      expect(translationPl.length).toBeGreaterThan(0)
      expect(translationEn.length).toBeGreaterThan(0)
    }
    expect(commonResources.pl.errors.generic).not.toBe('errors.generic')
    expect(commonResources.en.errors.generic).not.toBe('errors.generic')
  })
})

describe('applyApiException field errors', () => {
  it('a server error with a field pointer is shown on that field', () => {
    const { result } = renderForm()
    const error = new ApiException({
      status: 409,
      code: ErrorCode.SLUG_TAKEN,
      errors: [{ pointer: '/slug', code: ErrorCode.SLUG_TAKEN, params: {} }],
    })
    let message: string | null | undefined

    act(() => {
      message = applyApiException(result.current, error, pl)
    })

    expect(message).toBeNull()
    expect(result.current.errors.slug).toBe(pl('errors.slug_taken'))
  })

  it('only pointers that match a form field reach the fields', () => {
    const { result } = renderForm()
    const error = new ApiException({
      status: 400,
      code: ErrorCode.VALIDATION_FAILED,
      errors: [
        { pointer: '/slug', code: ErrorCode.SLUG_TAKEN, params: {} },
        { pointer: '/missing', code: ErrorCode.INVALID_NAME, params: { max: 100 } },
      ],
    })
    let message: string | null | undefined

    act(() => {
      message = applyApiException(result.current, error, pl)
    })

    expect(message).toBeNull()
    expect(result.current.errors).toEqual({ slug: pl('errors.slug_taken') })
  })
})

describe('applyApiException unmatched pointers', () => {
  it('a server error whose pointer matches no field shows the form message', () => {
    const { result } = renderForm()
    const error = new ApiException({
      status: 400,
      code: ErrorCode.VALIDATION_FAILED,
      errors: [{ pointer: '/missing', code: ErrorCode.INVALID_NAME, params: { max: 100 } }],
    })
    let message: string | null | undefined

    act(() => {
      message = applyApiException(result.current, error, pl)
    })

    expect(message).toBe(pl('errors.validation_failed'))
    expect(result.current.errors).toEqual({})
  })
})

describe('applyApiException form messages', () => {
  it('a server error without a field returns the form message', () => {
    const { result } = renderForm()
    const error = new ApiException({ status: 500, code: ErrorCode.INTERNAL_ERROR })
    let message: string | null | undefined

    act(() => {
      message = applyApiException(result.current, error, pl)
    })

    expect(message).toBe(pl('errors.internal_error'))
    expect(result.current.errors).toEqual({})
  })

  it('an unknown server code falls back to the generic message', () => {
    const { result } = renderForm()
    const error = new ApiException({ code: ClientErrorCode.UNKNOWN })
    let message: string | null | undefined

    act(() => {
      message = applyApiException(result.current, error, pl)
    })

    expect(message).toBe(pl('errors.generic'))
  })
})
