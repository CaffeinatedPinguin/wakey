import axios, { CanceledError } from 'axios'
import type { AxiosInstance } from 'axios'
import { describe, expect, it } from 'vitest'
import { installMockApi } from '../../test/mockApi'
import { createApiClient } from './client'
import { ApiError, isNotFoundError, isUnauthorizedError } from './problem'

function createClient(): AxiosInstance {
  return createApiClient({ baseURL: '/api/v1' })
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

describe('problem details mapping', () => {
  it('a problem+json error becomes an ApiError with code and field errors', async () => {
    installMockApi(() => ({
      status: 400,
      data: {
        type: 'about:blank',
        title: 'Bad Request',
        status: 400,
        detail: 'The request is invalid.',
        code: 'validation_failed',
        errors: [{ pointer: '/name', code: 'size', params: { min: 1, max: 100 } }],
      },
    }))

    const error = expectApiError(await failureOf(() => createClient().get('/spaces')))

    expect(error.status).toBe(400)
    expect(error.code).toBe('validation_failed')
    expect(error.detail).toBe('The request is invalid.')
    expect(error.errors[0]?.pointer).toBe('/name')
    expect(error.errors[0]?.params).toEqual({ min: 1, max: 100 })
  })

  it('a failure without a problem body becomes an ApiError with the unknown code', async () => {
    installMockApi(() => ({ status: 502, data: 'upstream gateway failure' }))

    const error = expectApiError(await failureOf(() => createClient().get('/spaces')))

    expect(error.status).toBe(502)
    expect(error.code).toBe('unknown')
    expect(error.errors).toEqual([])
  })
})

describe('problem params', () => {
  it('an ApiError carries the params of the problem', async () => {
    installMockApi(() => ({
      status: 400,
      data: {
        code: 'out_of_plan',
        params: { bound: 300, key: 'check.interval', planName: 'small' },
      },
    }))

    const error = expectApiError(await failureOf(() => createClient().get('/spaces')))

    expect(error.code).toBe('out_of_plan')
    expect(error.params).toEqual({ bound: 300, key: 'check.interval', planName: 'small' })
  })

  it('a problem without params gives an empty params object', async () => {
    installMockApi(() => ({ status: 404, data: { code: 'not_found', detail: 'No such space' } }))

    const error = expectApiError(await failureOf(() => createClient().get('/spaces/missing')))

    expect(error.code).toBe('not_found')
    expect(error.params).toEqual({})
  })
})

describe('error classification', () => {
  it('an unauthorized error is still recognised', async () => {
    installMockApi(() => ({ status: 401, data: { code: 'unauthenticated' } }))

    const failure = await failureOf(() => createClient().get('/me'))

    expect(isUnauthorizedError(failure)).toBe(true)
    expect(expectApiError(failure).code).toBe('unauthenticated')
  })

  it('a missing resource is recognised as not found', async () => {
    installMockApi(() => ({ status: 404, data: { code: 'not_found', detail: 'No such space' } }))

    const failure = await failureOf(() => createClient().get('/spaces/missing'))

    expect(isNotFoundError(failure)).toBe(true)
    expect(expectApiError(failure).code).toBe('not_found')
  })

  it('a cancelled request is not turned into an ApiError', async () => {
    const client = createClient()
    client.defaults.adapter = () => Promise.reject(new CanceledError('cancelled'))

    const failure = await failureOf(() => client.get('/me'))

    expect(axios.isCancel(failure)).toBe(true)
    expect(failure).not.toBeInstanceOf(ApiError)
  })
})
