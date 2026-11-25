import { contract } from '../../../shared/api/generated/contract'
import { serverClient } from '../../../shared/api/instances'
import { spaceAccessSchema, spaceSettingsSchema, settingsProblemSchema } from './settings.schemas'
import type { SpaceAccess, SpaceSetting } from './settings.schemas'

export interface SettingChange {
  key: string
  value: string
}

export interface SettingsWriteErrorInit {
  status: number
  code: string
  params: Readonly<Record<string, unknown>>
}

// A failed setting write keeps the Problem Details `params` (the plan bound),
// which the shared ApiError drops.
export class SettingsWriteError extends Error {
  readonly status: number
  readonly code: string
  readonly params: Readonly<Record<string, unknown>>

  constructor({ status, code, params }: SettingsWriteErrorInit) {
    super(`Setting write failed with code ${code}`)
    this.name = 'SettingsWriteError'
    this.status = status
    this.code = code
    this.params = params
  }
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

function failureFrom(data: unknown, status: number): SettingsWriteError {
  const parsed = settingsProblemSchema.safeParse(data)
  if (!parsed.success) {
    return new SettingsWriteError({ status, code: 'unknown', params: {} })
  }
  const field = parsed.data.errors[0]
  return new SettingsWriteError({
    status,
    code: parsed.data.code,
    params: { ...field?.params, ...parsed.data.params },
  })
}

function acceptsSettingWrite(status: number): boolean {
  return status === 204 || status === 400
}

export async function fetchSpaceSettings(
  slug: string,
  signal: AbortSignal,
): Promise<SpaceSetting[]> {
  const url = spacePath(contract.getSpaceSettings.path, slug)
  const response = await serverClient.get<unknown>(url, { signal })
  return spaceSettingsSchema.parse(response.data)
}

export async function fetchSpaceAccess(slug: string, signal: AbortSignal): Promise<SpaceAccess> {
  const url = spacePath(contract.getSpace.path, slug)
  const response = await serverClient.get<unknown>(url, { signal })
  return spaceAccessSchema.parse(response.data)
}

export async function updateSpaceSetting(slug: string, change: SettingChange): Promise<void> {
  const url = settingPath(contract.putSpaceSetting.path, slug, change.key)
  const response = await serverClient.put<unknown>(
    url,
    { value: change.value },
    { validateStatus: acceptsSettingWrite },
  )
  if (response.status === 204) {
    return
  }
  throw failureFrom(response.data, response.status)
}

export async function deleteSpaceSetting(slug: string, key: string): Promise<void> {
  const url = settingPath(contract.deleteSpaceSetting.path, slug, key)
  await serverClient.delete(url)
}

// "Restore all" is a sequence of the per-setting resets, so it needs no extra
// endpoint and leaves one audit entry per changed key.
export async function resetSpaceSettings(slug: string, keys: readonly string[]): Promise<void> {
  for (const key of keys) {
    await deleteSpaceSetting(slug, key)
  }
}
