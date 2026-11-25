// This file is generated, do not edit.
// Source: api/openapi/public.json

import type { components, operations } from './schema'

export type ProblemDetail = components['schemas']['ProblemDetail']
export type CountChecksResponse = operations['countChecks']['responses'][200]['content']['application/json']
export type CountChecksQuery = NonNullable<operations['countChecks']['parameters']['query']>
export type CountChecksPath = NonNullable<operations['countChecks']['parameters']['path']>
export type CreateCheckResponse = operations['createCheck']['responses'][201]['content']['application/json']
export type CreateCheckRequest = operations['createCheck']['requestBody']['content']['application/json']
export type CreateCheckPath = NonNullable<operations['createCheck']['parameters']['path']>
export type CreateTokenResponse = operations['createToken']['responses'][201]['content']['application/json']
export type CreateTokenRequest = operations['createToken']['requestBody']['content']['application/json']
export type CreateTokenPath = NonNullable<operations['createToken']['parameters']['path']>
export type DeleteCheckPath = NonNullable<operations['deleteCheck']['parameters']['path']>
export type DeleteSpaceSettingPath = NonNullable<operations['deleteSpaceSetting']['parameters']['path']>
export type GetCheckResponse = operations['getCheck']['responses'][200]['content']['application/json']
export type GetCheckPath = NonNullable<operations['getCheck']['parameters']['path']>
export type GetMeResponse = operations['getMe']['responses'][200]['content']['application/json']
export type GetSpaceResponse = operations['getSpace']['responses'][200]['content']['application/json']
export type GetSpacePath = NonNullable<operations['getSpace']['parameters']['path']>
export type GetSpaceSettingsResponse = operations['getSpaceSettings']['responses'][200]['content']['application/json']
export type GetSpaceSettingsPath = NonNullable<operations['getSpaceSettings']['parameters']['path']>
export type ListChecksResponse = operations['listChecks']['responses'][200]['content']['application/json']
export type ListChecksQuery = NonNullable<operations['listChecks']['parameters']['query']>
export type ListChecksPath = NonNullable<operations['listChecks']['parameters']['path']>
export type ListSpacesResponse = operations['listSpaces']['responses'][200]['content']['application/json']
export type ListSpacesQuery = NonNullable<operations['listSpaces']['parameters']['query']>
export type ListTokensResponse = operations['listTokens']['responses'][200]['content']['application/json']
export type ListTokensPath = NonNullable<operations['listTokens']['parameters']['path']>
export type PauseCheckResponse = operations['pauseCheck']['responses'][200]['content']['application/json']
export type PauseCheckRequest = operations['pauseCheck']['requestBody']['content']['application/json']
export type PauseCheckPath = NonNullable<operations['pauseCheck']['parameters']['path']>
export type PutSpaceResponse = operations['putSpace']['responses'][200]['content']['application/json']
export type PutSpaceRequest = operations['putSpace']['requestBody']['content']['application/json']
export type PutSpacePath = NonNullable<operations['putSpace']['parameters']['path']>
export type PutSpacePlanRequest = operations['putSpacePlan']['requestBody']['content']['application/json']
export type PutSpacePlanPath = NonNullable<operations['putSpacePlan']['parameters']['path']>
export type PutSpaceSettingRequest = operations['putSpaceSetting']['requestBody']['content']['application/json']
export type PutSpaceSettingPath = NonNullable<operations['putSpaceSetting']['parameters']['path']>
export type ResumeCheckResponse = operations['resumeCheck']['responses'][200]['content']['application/json']
export type ResumeCheckPath = NonNullable<operations['resumeCheck']['parameters']['path']>
export type RevokeTokenPath = NonNullable<operations['revokeToken']['parameters']['path']>
export type UpdateCheckResponse = operations['updateCheck']['responses'][200]['content']['application/json']
export type UpdateCheckRequest = operations['updateCheck']['requestBody']['content']['application/json']
export type UpdateCheckPath = NonNullable<operations['updateCheck']['parameters']['path']>
