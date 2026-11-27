import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { QueryClient, UseMutationResult, UseQueryResult } from '@tanstack/react-query'
import {
  deleteSpaceSetting,
  fetchSpaceSettings,
  resetAllSpaceSettings,
  updateSpaceSetting,
} from './settings.api'
import type { SettingChange } from './settings.api'
import type { SpaceSetting } from './settings.schemas'

const SETTINGS_KEY = 'settings'
const SETTINGS_MUTATION_SCOPE = { id: 'settings-change' }

export interface RestoreHandlers {
  onError: (error: Error) => void
  onSettled: () => void
}

export const settingsQueryKeys = {
  space: (slug: string) => [SETTINGS_KEY, 'bySpace', slug] as const,
}

function invalidateSettings(queryClient: QueryClient, slug: string | undefined): void {
  if (slug === undefined) {
    return
  }
  void queryClient.invalidateQueries({ queryKey: settingsQueryKeys.space(slug) })
}

export function useSpaceSettings(slug: string | undefined): UseQueryResult<SpaceSetting[]> {
  return useQuery({
    queryKey: settingsQueryKeys.space(slug ?? ''),
    queryFn: ({ signal }) => {
      if (slug === undefined) {
        throw new Error('A slug is required to read the settings')
      }
      return fetchSpaceSettings(slug, signal)
    },
    enabled: slug !== undefined,
  })
}

function requireSlug(slug: string | undefined): string {
  if (slug === undefined) {
    throw new Error('A slug is required to change the settings')
  }
  return slug
}

export function useChangeSetting(
  slug: string | undefined,
): UseMutationResult<void, Error, SettingChange> {
  const queryClient = useQueryClient()
  return useMutation({
    // A save of one setting waits for the previous save, so a late response
    // cannot overwrite a newer value.
    scope: SETTINGS_MUTATION_SCOPE,
    mutationFn: (change: SettingChange) => updateSpaceSetting(requireSlug(slug), change),
    onSuccess: () => {
      invalidateSettings(queryClient, slug)
    },
  })
}

export function useRestoreSetting(
  slug: string | undefined,
  handlers: RestoreHandlers,
): UseMutationResult<void, Error, string> {
  const queryClient = useQueryClient()
  return useMutation({
    scope: SETTINGS_MUTATION_SCOPE,
    mutationFn: (key: string) => deleteSpaceSetting(requireSlug(slug), key),
    onError: handlers.onError,
    onSettled: () => {
      handlers.onSettled()
      invalidateSettings(queryClient, slug)
    },
  })
}

export function useRestoreAllSettings(
  slug: string | undefined,
  handlers: RestoreHandlers,
): UseMutationResult<void, Error, void> {
  const queryClient = useQueryClient()
  return useMutation({
    scope: SETTINGS_MUTATION_SCOPE,
    mutationFn: () => resetAllSpaceSettings(requireSlug(slug)),
    onError: handlers.onError,
    onSettled: () => {
      handlers.onSettled()
      invalidateSettings(queryClient, slug)
    },
  })
}
