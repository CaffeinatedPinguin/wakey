import { z } from 'zod'
import type { GetMeResponse } from '../../../shared/api/generated/types'

export const meSchema = z.object({
  issuer: z.string(),
  subject: z.string(),
  admin: z.boolean(),
}) satisfies z.ZodType<GetMeResponse>

export type Me = z.infer<typeof meSchema>
