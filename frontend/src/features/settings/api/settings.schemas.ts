import { z } from 'zod'
import {
  Language,
  SettingGroup,
  SettingSource,
  SettingType,
} from '../../../shared/api/generated/enums'
import type {
  GetSpaceResponse,
  GetSpaceSettingsResponse,
} from '../../../shared/api/generated/types'
import type { ApiShape } from '../../../shared/api/apiShape'

export const settingSourceSchema = z.enum(Object.values(SettingSource))
export const settingTypeSchema = z.enum(Object.values(SettingType))
export const settingGroupSchema = z.enum(Object.values(SettingGroup))
export const languageSchema = z.enum(Object.values(Language))

export const spaceSettingSchema = z.object({
  key: z.string(),
  planName: z.string(),
  source: settingSourceSchema,
  value: z.string(),
  type: settingTypeSchema,
  unit: z.string().nullable().optional(),
  options: z.array(languageSchema).nullable().optional(),
  min: z.number().nullable().optional(),
  max: z.number().nullable().optional(),
  group: settingGroupSchema,
}) satisfies z.ZodType<ApiShape<GetSpaceSettingsResponse['items'][number]>>

// The server returns every setting on one page; nextCursor is part of the contract.
export const spaceSettingsPageSchema = z.object({
  items: z.array(spaceSettingSchema),
  nextCursor: z.string().nullable(),
}) satisfies z.ZodType<ApiShape<GetSpaceSettingsResponse>>

// The caller's access level for one space, taken from the shared space detail.
export type SpaceAccess = Pick<GetSpaceResponse, 'role'>

export type SpaceSetting = z.infer<typeof spaceSettingSchema>
