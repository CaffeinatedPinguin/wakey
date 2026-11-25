import { z } from 'zod'
import type {
  GetSpaceResponse,
  GetSpaceSettingsResponse,
} from '../../../shared/api/generated/types'
import { problemSchema } from '../../../shared/api/problem'

export const settingSourceSchema = z.enum(['SPACE', 'PLAN', 'INSTANCE', 'DEFAULT'])

export const spaceSettingSchema = z.object({
  key: z.string(),
  planName: z.string(),
  source: settingSourceSchema,
  value: z.string(),
}) satisfies z.ZodType<GetSpaceSettingsResponse[number]>

export const spaceSettingsSchema = z.array(
  spaceSettingSchema,
) satisfies z.ZodType<GetSpaceSettingsResponse>

// The caller's access level for one space, taken from the same response the
// shell uses to resolve the space in the URL.
export type SpaceAccess = Pick<GetSpaceResponse, 'admin' | 'role'>

export const spaceAccessSchema = z.object({
  admin: z.boolean(),
  role: z.enum(['EDITOR', 'VIEWER']).nullable(),
}) satisfies z.ZodType<SpaceAccess>

// The settings endpoints put the plan bound in a top-level `params` object,
// which the shared problem schema does not model.
export const settingsProblemSchema = problemSchema.extend({
  params: z.record(z.string(), z.unknown()).default({}),
})

export type SpaceSetting = z.infer<typeof spaceSettingSchema>
export type SettingsProblem = z.infer<typeof settingsProblemSchema>
