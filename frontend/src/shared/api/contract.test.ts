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

    expectTypeOf<GetMeResponse>().toEqualTypeOf<{ admin: boolean; subject: string }>()
    expectTypeOf<GetSpaceResponse>().toEqualTypeOf<{
      admin: boolean
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
    expectTypeOf<PutSpaceRequest>().toEqualTypeOf<{ name: string }>()
    expectTypeOf<PutSpaceResponse>().toEqualTypeOf<{
      admin: boolean
      name: string
      role: 'EDITOR' | 'VIEWER' | null
      slug: string
      version: number
    }>()
    expectTypeOf<PutSpacePath>().toEqualTypeOf<{ slug: string }>()
    expectTypeOf<ProblemDetail>().toExtend<{ code?: string; detail?: string }>()
  })
})
