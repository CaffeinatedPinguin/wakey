import axios, { CanceledError } from 'axios'
import type { AxiosInstance } from 'axios'
import { describe, expect, it } from 'vitest'
import { installMockApi } from '../../test/mockApi'
import { appConfig } from '../config'
import {
  ApiException,
  ClientErrorCode,
  isNotFoundException,
  isUnauthorizedException,
} from './apiException'
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

function expectApiException(value: unknown): ApiException {
  if (!(value instanceof ApiException)) {
    throw new Error('Expected an ApiException')
  }
  return value
}

const VALIDATION_FAILURE = {
  status: 400,
  data: {
    type: 'about:blank',
    title: 'Bad Request',
    status: 400,
    detail: 'The request is invalid.',
    code: ErrorCode.VALIDATION_FAILED,
    errors: [{ pointer: '/name', code: 'size', params: { min: 1, max: 100 } }],
  },
}

describe('API exception mapping', () => {
  it('the API exception keeps the code, the field errors and the status of the response', async () => {
    installMockApi(() => VALIDATION_FAILURE)

    const error = expectApiException(await failureOf(() => createClient().get('/spaces')))

    expect(error.status).toBe(400)
    expect(error.code).toBe(ErrorCode.VALIDATION_FAILED)
    expect(error.detail).toBe('The request is invalid.')
    expect(error.errors[0]?.pointer).toBe('/name')
    expect(error.errors[0]?.params).toEqual({ min: 1, max: 100 })
  })

  it('a failure without an error body becomes an ApiException with the unknown code', async () => {
    installMockApi(() => ({ status: 502, data: 'upstream gateway failure' }))

    const error = expectApiException(await failureOf(() => createClient().get('/spaces')))

    expect(error.status).toBe(502)
    expect(error.code).toBe(ClientErrorCode.UNKNOWN)
    expect(error.errors).toEqual([])
  })

  it('an unknown server error code becomes the client unknown code', async () => {
    installMockApi(() => ({ status: 400, data: { code: 'future_code' } }))

    const error = expectApiException(await failureOf(() => createClient().get('/spaces')))

    expect(error.code).toBe(ClientErrorCode.UNKNOWN)
  })
})

describe('exception classification', () => {
  it('an unauthorized error is still recognised', async () => {
    installMockApi(() => ({ status: 401, data: { code: ErrorCode.UNAUTHENTICATED } }))

    const failure = await failureOf(() => createClient().get('/me'))

    expect(isUnauthorizedException(failure)).toBe(true)
    expect(expectApiException(failure).code).toBe(ErrorCode.UNAUTHENTICATED)
  })

  it('a missing resource is recognised as not found', async () => {
    installMockApi(() => ({
      status: 404,
      data: { code: ErrorCode.NOT_FOUND, detail: 'No such space' },
    }))

    const failure = await failureOf(() => createClient().get('/spaces/missing'))

    expect(isNotFoundException(failure)).toBe(true)
    expect(expectApiException(failure).code).toBe(ErrorCode.NOT_FOUND)
  })

  it('a cancelled request is not turned into an ApiException', async () => {
    const client = createClient()
    client.defaults.adapter = () => Promise.reject(new CanceledError('cancelled'))

    const failure = await failureOf(() => client.get('/me'))

    expect(axios.isCancel(failure)).toBe(true)
    expect(failure).not.toBeInstanceOf(ApiException)
  })
})
