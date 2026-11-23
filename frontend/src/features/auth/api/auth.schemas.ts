import { z } from 'zod'

export const meSchema = z.object({
  subject: z.string(),
  admin: z.boolean(),
})

export type Me = z.infer<typeof meSchema>
