import type { Me } from '../features/auth/api/auth.schemas'

export const signedInUser: Me = {
  subject: 'alice',
  admin: true,
}

export const unauthorizedBody = { code: 'unauthorized' }
