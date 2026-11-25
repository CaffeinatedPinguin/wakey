import { z } from 'zod'
import { SpaceRole } from '../../../shared/api/generated/enums'
import type { GetSpaceResponse, ListSpacesResponse } from '../../../shared/api/generated/types'

export const spaceSchema = z.object({
  name: z.string(),
  role: z.enum(SpaceRole).nullable(),
  slug: z.string(),
  version: z.number().int(),
}) satisfies z.ZodType<GetSpaceResponse>

// Every field in the contract is required; nextCursor is explicitly nullable.
export const spacesPageSchema = z.object({
  items: z.array(spaceSchema),
  nextCursor: z.string().nullable(),
}) satisfies z.ZodType<ListSpacesResponse>
