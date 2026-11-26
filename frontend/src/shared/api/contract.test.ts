import { describe, expect, expectTypeOf, it } from 'vitest'
import { contract } from './generated/contract'
import type { OperationId } from './generated/contract'
import { ErrorCode, SpaceRole } from './generated/enums'
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
    expect(contract.getMe.method).toBe('get')
    expect(contract.createSpace.method).toBe('post')
    expect(contract.putSpace.method).toBe('put')
    expect(contract.getSpace.path).toBe(contract.putSpace.path)
  })
})

describe('generated closed sets', () => {
  it('pins the wire values the server sends', () => {
    expect(ErrorCode.SLUG_TAKEN).toBe('slug_taken')
    expect(ErrorCode.VALIDATION_FAILED).toBe('validation_failed')
    expect(ErrorCode.INVALID_SLUG).toBe('invalid_slug')
    expect(ErrorCode.NOT_FOUND).toBe('not_found')
    expect(SpaceRole.VIEWER).toBe('VIEWER')
    expect(SpaceRole.EDITOR).toBe('EDITOR')
  })
})

describe('generated API types', () => {
  it('aliases every operation type', () => {
    expectTypeOf<GetMeResponse>().toEqualTypeOf<{
      admin: boolean
      issuer: string
      subject: string
    }>()
    expectTypeOf<CreateSpaceResponse>().toEqualTypeOf<GetSpaceResponse>()
    expectTypeOf<CreateSpaceRequest>().toEqualTypeOf<{ name: string; slug: string }>()
    expectTypeOf<GetSpaceResponse>().toEqualTypeOf<{
      name: string
      role: SpaceRole | null
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
      role: SpaceRole | null
      slug: string
      version: number
    }>()
    expectTypeOf<PutSpacePath>().toEqualTypeOf<{ slug: string }>()
    expectTypeOf<ProblemDetail>().toExtend<{ code?: string; detail?: string }>()
  })
})
