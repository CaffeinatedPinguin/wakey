import axios, { CanceledError } from 'axios'
import type { AxiosInstance } from 'axios'
import { describe, expect, it } from 'vitest'
import { installMockApi } from '../../test/mockApi'
import { appConfig } from '../config'
import { ApiError, ClientErrorCode, isNotFoundError, isUnauthorizedError } from './apiError'
import { createApiClient } from './client'
import { ErrorCode } from './generated/enums'

function createClient(): AxiosInstance {
  return createApiClient({ baseURL: appConfig.apiBaseUrl })
}

async function failureOf(request: () => Promise<unknown>): Promise<unknown> {
  try {
    await request()
  } catch (error) {
    return error
  }
  throw new Error('The request was expected to fail')
}

function expectApiError(value: unknown): ApiError {
  if (!(value instanceof ApiError)) {
    throw new Error('Expected an ApiError')
  }
  return value
}

describe('API error mapping', () => {
  it('a problem+json error becomes an ApiError with code and field errors', async () => {
    installMockApi(() => ({
      status: 400,
      data: {
        type: 'about:blank',
        title: 'Bad Request',
        status: 400,
        detail: 'The request is invalid.',
        code: ErrorCode.VALIDATION_FAILED,
        errors: [{ pointer: '/name', code: 'size', params: { min: 1, max: 100 } }],
      },
    }))

    const error = expectApiError(await failureOf(() => createClient().get('/spaces')))

    expect(error.status).toBe(400)
    expect(error.code).toBe(ErrorCode.VALIDATION_FAILED)
    expect(error.detail).toBe('The request is invalid.')
    expect(error.errors[0]?.pointer).toBe('/name')
    expect(error.errors[0]?.params).toEqual({ min: 1, max: 100 })
  })
  it('a failure without a problem body becomes an ApiError with the unknown code', async () => {
    installMockApi(() => ({ status: 502, data: 'upstream gateway failure' }))

    const error = expectApiError(await failureOf(() => createClient().get('/spaces')))

    expect(error.status).toBe(502)
    expect(error.code).toBe(ClientErrorCode.UNKNOWN)
    expect(error.errors).toEqual([])
  })

  it('an unknown server error code becomes the client unknown code', async () => {
    installMockApi(() => ({ status: 400, data: { code: 'future_code' } }))

    const error = expectApiError(await failureOf(() => createClient().get('/spaces')))

    expect(error.code).toBe(ClientErrorCode.UNKNOWN)
  })
})

describe('error classification', () => {
  it('an unauthorized error is still recognised', async () => {
    installMockApi(() => ({ status: 401, data: { code: ErrorCode.UNAUTHENTICATED } }))

    const failure = await failureOf(() => createClient().get('/me'))

    expect(isUnauthorizedError(failure)).toBe(true)
    expect(expectApiError(failure).code).toBe(ErrorCode.UNAUTHENTICATED)
  })

  it('a missing resource is recognised as not found', async () => {
    installMockApi(() => ({
      status: 404,
      data: { code: ErrorCode.NOT_FOUND, detail: 'No such space' },
    }))

    const failure = await failureOf(() => createClient().get('/spaces/missing'))

    expect(isNotFoundError(failure)).toBe(true)
    expect(expectApiError(failure).code).toBe(ErrorCode.NOT_FOUND)
  })

  it('a cancelled request is not turned into an ApiError', async () => {
    const client = createClient()
    client.defaults.adapter = () => Promise.reject(new CanceledError('cancelled'))

    const failure = await failureOf(() => client.get('/me'))

    expect(axios.isCancel(failure)).toBe(true)
    expect(failure).not.toBeInstanceOf(ApiError)
  })
})
