import { z } from 'zod'
import type {
  GetSpaceResponse,
  GetSpaceSettingsResponse,
} from '../../../shared/api/generated/types'

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

// The caller's access level for one space, taken from the shared space detail.
export type SpaceAccess = Pick<GetSpaceResponse, 'role'>

export type SpaceSetting = z.infer<typeof spaceSettingSchema>
