import { contract } from '../../../shared/api/generated/contract'
import { serverClient } from '../../../shared/api/instances'
import { operationPath } from '../../../shared/api/operationPath'
import { spaceSettingsPageSchema } from './settings.schemas'
import type { SpaceSetting } from './settings.schemas'

export interface SettingChange {
  key: string
  value: string
}

export async function fetchSpaceSettings(
  slug: string,
  signal: AbortSignal,
): Promise<SpaceSetting[]> {
  const url = operationPath(contract.getSpaceSettings.path, { slug })
  const response = await serverClient.get<unknown>(url, { signal })
  return spaceSettingsPageSchema.parse(response.data).items
}

export async function updateSpaceSetting(slug: string, change: SettingChange): Promise<void> {
  const url = operationPath(contract.putSpaceSetting.path, { slug, key: change.key })
  await serverClient.put<unknown>(url, { value: change.value })
}

export async function deleteSpaceSetting(slug: string, key: string): Promise<void> {
  const url = operationPath(contract.deleteSpaceSetting.path, { slug, key })
  await serverClient.delete(url)
}

export async function resetAllSpaceSettings(slug: string): Promise<void> {
  const url = operationPath(contract.resetSpaceSettings.path, { slug })
  await serverClient.delete(url)
}
