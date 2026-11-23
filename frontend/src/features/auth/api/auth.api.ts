import { apiClient } from '../../../shared/api/client'
import { meSchema } from './auth.schemas'
import type { Me } from './auth.schemas'

export const LOGIN_PATH = '/oauth2/authorization/oidc'
export const LOGOUT_PATH = '/logout'
export const ME_PATH = '/api/v1/me'

export async function fetchMe(): Promise<Me> {
  const response = await apiClient.get<unknown>(ME_PATH)
  return meSchema.parse(response.data)
}

export async function logout(): Promise<void> {
  await apiClient.post(LOGOUT_PATH)
}
