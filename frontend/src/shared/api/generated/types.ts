// This file is generated, do not edit.
// Source: api/openapi/public.json

import type { components, operations } from './schema'

export type ProblemDetail = components['schemas']['ProblemDetail']
export type GetMeResponse = operations['getMe']['responses'][200]['content']['application/json']
export type GetSpaceResponse = operations['getSpace']['responses'][200]['content']['application/json']
export type GetSpacePath = NonNullable<operations['getSpace']['parameters']['path']>
export type ListSpacesResponse = operations['listSpaces']['responses'][200]['content']['application/json']
export type ListSpacesQuery = NonNullable<operations['listSpaces']['parameters']['query']>
export type PutSpaceResponse = operations['putSpace']['responses'][200]['content']['application/json']
export type PutSpaceRequest = operations['putSpace']['requestBody']['content']['application/json']
export type PutSpacePath = NonNullable<operations['putSpace']['parameters']['path']>
