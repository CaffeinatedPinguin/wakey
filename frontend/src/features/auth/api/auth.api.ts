import { apiClient, serverClient } from '../../../shared/api/instances'
import { appConfig } from '../../../shared/config'
import { meSchema } from './auth.schemas'
import type { Me } from './auth.schemas'

export const ME_PATH = '/me'

export async function fetchMe(): Promise<Me> {
  const response = await apiClient.get<unknown>(ME_PATH)
  return meSchema.parse(response.data)
}

export async function logout(): Promise<void> {
  await serverClient.post(appConfig.logoutUrl)
}
