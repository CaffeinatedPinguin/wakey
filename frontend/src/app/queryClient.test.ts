import { describe, expect, it } from 'vitest'
import { ErrorCode } from '../shared/api/generated/enums'
import { serverClient } from '../shared/api/instances'
import { installMockApi } from '../test/mockApi'
import { createQueryClient } from './queryClient'

describe('query retry policy', () => {
  it('does not retry a 401 response', async () => {
    let requests = 0
    installMockApi(() => {
      requests += 1
      return { status: 401, data: { code: ErrorCode.UNAUTHENTICATED } }
    })
    const queryClient = createQueryClient()

    await expect(
      queryClient.query({
        queryKey: ['retry', 'unauthorized'],
        queryFn: () => serverClient.get('/me'),
      }),
    ).rejects.toBeTruthy()

    expect(requests).toBe(1)
  })

  it('retries a network error at most twice', async () => {
    let requests = 0
    installMockApi(() => {
      requests += 1
      return { status: 0, networkError: true }
    })
    const queryClient = createQueryClient()

    await expect(
      queryClient.query({
        queryKey: ['retry', 'network'],
        queryFn: () => serverClient.get('/me'),
        retryDelay: 0,
      }),
    ).rejects.toBeTruthy()

    expect(requests).toBe(3)
  })
})
