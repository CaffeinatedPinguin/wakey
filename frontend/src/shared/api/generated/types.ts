// This file is generated, do not edit.
// Source: api/openapi/public.json

import type { components, operations } from './schema'

export type ProblemDetail = components['schemas']['ProblemDetail']
export type AddSpaceMemberResponse = operations['addSpaceMember']['responses'][201]['content']['application/json']
export type AddSpaceMemberRequest = operations['addSpaceMember']['requestBody']['content']['application/json']
export type AddSpaceMemberPath = NonNullable<operations['addSpaceMember']['parameters']['path']>
export type ChangeSpaceMemberRoleRequest = operations['changeSpaceMemberRole']['requestBody']['content']['application/json']
export type ChangeSpaceMemberRoleQuery = NonNullable<operations['changeSpaceMemberRole']['parameters']['query']>
export type ChangeSpaceMemberRolePath = NonNullable<operations['changeSpaceMemberRole']['parameters']['path']>
export type CreateSpaceResponse = operations['createSpace']['responses'][201]['content']['application/json']
export type CreateSpaceRequest = operations['createSpace']['requestBody']['content']['application/json']
export type GetMeResponse = operations['getMe']['responses'][200]['content']['application/json']
export type GetSpaceResponse = operations['getSpace']['responses'][200]['content']['application/json']
export type GetSpacePath = NonNullable<operations['getSpace']['parameters']['path']>
export type ListSpaceMembersResponse = operations['listSpaceMembers']['responses'][200]['content']['application/json']
export type ListSpaceMembersQuery = NonNullable<operations['listSpaceMembers']['parameters']['query']>
export type ListSpaceMembersPath = NonNullable<operations['listSpaceMembers']['parameters']['path']>
export type ListSpacesResponse = operations['listSpaces']['responses'][200]['content']['application/json']
export type ListSpacesQuery = NonNullable<operations['listSpaces']['parameters']['query']>
export type PutSpaceResponse = operations['putSpace']['responses'][200]['content']['application/json']
export type PutSpaceRequest = operations['putSpace']['requestBody']['content']['application/json']
export type PutSpacePath = NonNullable<operations['putSpace']['parameters']['path']>
export type RemoveSpaceMemberQuery = NonNullable<operations['removeSpaceMember']['parameters']['query']>
export type RemoveSpaceMemberPath = NonNullable<operations['removeSpaceMember']['parameters']['path']>
