import { describe, expect, expectTypeOf, it } from 'vitest'
import { contract } from './generated/contract'
import type { OperationId } from './generated/contract'
import type {
  CreateSpaceRequest,
  CreateSpaceResponse,
  GetMeResponse,
  GetSpacePath,
  GetSpaceResponse,
  ListSpacesQuery,
  ListSpacesResponse,
  ProblemDetail,
  PutSpacePath,
  PutSpaceRequest,
  PutSpaceResponse,
} from './generated/types'

describe('generated API operations', () => {
  it('exposes every operation', () => {
    const operationIds: OperationId[] = [
      'createSpace',
      'getMe',
      'getSpace',
      'listSpaces',
      'putSpace',
    ]

    expect(operationIds).toHaveLength(5)
    for (const operationId of operationIds) {
      expect(contract[operationId]).toBeDefined()
    }
    expect(contract.getMe).toEqual({ method: 'get', path: '/api/v1/me' })
    expect(contract.createSpace).toEqual({ method: 'post', path: '/api/v1/spaces' })
    expect(contract.putSpace).toEqual({ method: 'put', path: '/api/v1/spaces/{slug}' })
  })
})

describe('generated API types', () => {
  it('aliases every operation type', () => {
    expectTypeOf<GetMeResponse>().toEqualTypeOf<{ admin: boolean; subject: string }>()
    expectTypeOf<CreateSpaceResponse>().toEqualTypeOf<GetSpaceResponse>()
    expectTypeOf<CreateSpaceRequest>().toEqualTypeOf<{ name: string; slug: string }>()
    expectTypeOf<GetSpaceResponse>().toEqualTypeOf<{
      name: string
      role: 'EDITOR' | 'VIEWER' | null
      slug: string
      version: number
    }>()
    expectTypeOf<GetSpacePath>().toEqualTypeOf<{ slug: string }>()
    expectTypeOf<ListSpacesQuery>().toExtend<{ limit?: number; cursor?: string }>()
    expectTypeOf<ListSpacesResponse>().toEqualTypeOf<{
      items: GetSpaceResponse[]
      nextCursor: string | null
    }>()
    expectTypeOf<PutSpaceRequest>().toEqualTypeOf<{
      name: string
      version?: number
    }>()
    expectTypeOf<PutSpaceResponse>().toEqualTypeOf<{
      name: string
      role: 'EDITOR' | 'VIEWER' | null
      slug: string
      version: number
    }>()
    expectTypeOf<PutSpacePath>().toEqualTypeOf<{ slug: string }>()
    expectTypeOf<ProblemDetail>().toExtend<{ code?: string; detail?: string }>()
  })
})
