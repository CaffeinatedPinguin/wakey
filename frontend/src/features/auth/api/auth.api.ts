import { contract } from '../../../shared/api/generated/contract'
import { serverClient } from '../../../shared/api/instances'
import { appConfig } from '../../../shared/config'
import { meSchema } from './auth.schemas'
import type { Me } from './auth.schemas'

export async function fetchMe(): Promise<Me> {
  const response = await serverClient.get<unknown>(contract.getMe.path)
  return meSchema.parse(response.data)
}

export async function logout(): Promise<void> {
  await serverClient.post(appConfig.logoutUrl)
}
