import { contract } from '../../../shared/api/generated/contract'
import { serverClient } from '../../../shared/api/instances'
import { spaceSettingsPageSchema } from './settings.schemas'
import type { SpaceSetting } from './settings.schemas'

export interface SettingChange {
  key: string
  value: string
}

function pathWith(path: string, name: string, value: string): string {
  return path.replace(`{${name}}`, encodeURIComponent(value))
}

function spacePath(path: string, slug: string): string {
  return pathWith(path, 'slug', slug)
}

function settingPath(path: string, slug: string, key: string): string {
  return pathWith(spacePath(path, slug), 'key', key)
}

export async function fetchSpaceSettings(
  slug: string,
  signal: AbortSignal,
): Promise<SpaceSetting[]> {
  const url = spacePath(contract.getSpaceSettings.path, slug)
  const response = await serverClient.get<unknown>(url, { signal })
  return spaceSettingsPageSchema.parse(response.data).items
}

export async function updateSpaceSetting(slug: string, change: SettingChange): Promise<void> {
  const url = settingPath(contract.putSpaceSetting.path, slug, change.key)
  await serverClient.put<unknown>(url, { value: change.value })
}

export async function deleteSpaceSetting(slug: string, key: string): Promise<void> {
  const url = settingPath(contract.deleteSpaceSetting.path, slug, key)
  await serverClient.delete(url)
}

export async function resetAllSpaceSettings(slug: string): Promise<void> {
  const url = spacePath(contract.resetSpaceSettings.path, slug)
  await serverClient.delete(url)
}
