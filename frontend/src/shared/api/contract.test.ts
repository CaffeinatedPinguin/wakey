import { describe, expect, expectTypeOf, it } from 'vitest'
import { contract } from './generated/contract'
import type { OperationId } from './generated/contract'
import type {
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

describe('generated API surface', () => {
  it('types are aliased per operationId', () => {
    const operationIds: OperationId[] = ['getMe', 'getSpace', 'listSpaces', 'putSpace']

    expect(operationIds).toHaveLength(4)
    for (const operationId of operationIds) {
      expect(contract[operationId]).toBeDefined()
    }
    expect(contract.getMe).toEqual({ method: 'get', path: '/api/v1/me' })
    expect(contract.putSpace).toEqual({ method: 'put', path: '/api/v1/spaces/{slug}' })

    expectTypeOf<GetMeResponse>().toExtend<{ subject?: string; admin?: boolean }>()
    expectTypeOf<GetSpaceResponse>().toExtend<{ slug?: string; name?: string }>()
    expectTypeOf<GetSpacePath>().toEqualTypeOf<{ slug: string }>()
    expectTypeOf<ListSpacesQuery>().toExtend<{ limit?: number; cursor?: string }>()
    expectTypeOf<ListSpacesResponse>().toExtend<{ items?: unknown[]; nextCursor?: string }>()
    expectTypeOf<PutSpaceRequest>().toEqualTypeOf<{ name: string }>()
    expectTypeOf<PutSpaceResponse>().toExtend<{ slug?: string; name?: string }>()
    expectTypeOf<PutSpacePath>().toEqualTypeOf<{ slug: string }>()
    expectTypeOf<ProblemDetail>().toExtend<{ code?: string; detail?: string }>()
  })
})
