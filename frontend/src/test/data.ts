import type { Me } from '../features/auth/api/auth.schemas'
import { ErrorCode } from '../shared/api/generated/enums'

export const signedInUser: Me = {
  issuer: 'https://idp.example.test',
  subject: 'alice',
  admin: true,
}

export const unauthorizedBody = { code: ErrorCode.UNAUTHENTICATED }
