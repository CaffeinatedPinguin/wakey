import { z } from 'zod'
import type {
  GetSpaceResponse,
  ListSpacesResponse,
  PutSpaceResponse,
} from '../../../shared/api/generated/types'

export const spaceSchema = z.object({
  slug: z.string(),
  name: z.string(),
}) satisfies z.ZodType<GetSpaceResponse>

export type Space = z.infer<typeof spaceSchema>

// The panel needs only the current page; richer fields from the contract stay
// out of the view model.
export const spacesPageSchema = z.object({
  items: z.array(spaceSchema),
}) satisfies z.ZodType<ListSpacesResponse>

export const createdSpaceSchema = spaceSchema satisfies z.ZodType<PutSpaceResponse>
