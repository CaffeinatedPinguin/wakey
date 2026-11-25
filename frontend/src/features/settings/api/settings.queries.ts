import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { QueryClient, UseMutationResult, UseQueryResult } from '@tanstack/react-query'
import { fetchSpaceSettings, resetSpaceSettings, updateSpaceSetting } from './settings.api'
import type { SettingChange } from './settings.api'
import type { SpaceSetting } from './settings.schemas'

const SETTINGS_KEY = 'settings'

export const settingsQueryKeys = {
  space: (slug: string) => [SETTINGS_KEY, 'space', slug] as const,
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
    mutationFn: (change: SettingChange) => updateSpaceSetting(requireSlug(slug), change),
    onSuccess: () => {
      invalidateSettings(queryClient, slug)
    },
  })
}

export function useRestoreSetting(
  slug: string | undefined,
): UseMutationResult<void, Error, string> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (key: string) => resetSpaceSettings(requireSlug(slug), [key]),
    onSuccess: () => {
      invalidateSettings(queryClient, slug)
    },
  })
}

export function useRestoreAllSettings(
  slug: string | undefined,
): UseMutationResult<void, Error, readonly string[]> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (keys: readonly string[]) => resetSpaceSettings(requireSlug(slug), keys),
    onSuccess: () => {
      invalidateSettings(queryClient, slug)
    },
  })
}
