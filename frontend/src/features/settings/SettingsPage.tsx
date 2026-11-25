import type { ReactElement } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useQueryClient } from '@tanstack/react-query'
import type { QueryClient, UseQueryResult } from '@tanstack/react-query'
import { GeneralDefaultsView } from './GeneralDefaultsView'
import type { SpaceAccess, SpaceSetting } from './api/settings.schemas'
import {
  settingsQueryKeys,
  useChangeSetting,
  useRestoreAllSettings,
  useRestoreSetting,
  useSpaceAccess,
  useSpaceSettings,
} from './api/settings.queries'
import {
  buildSettingsGroups,
  canEditSpace,
  overriddenSettingKeys,
  settingFieldError,
} from './settingsModel'
import type { SettingFieldError, SettingFieldErrors, SettingsStatus } from './types'

export interface SettingsPageProps {
  spaceSlug: string | undefined
}

const NO_FIELD_ERRORS: SettingFieldErrors = {}

function withoutKey(errors: SettingFieldErrors, key: string): SettingFieldErrors {
  if (errors[key] === undefined) {
    return errors
  }
  const next: Record<string, SettingFieldError> = {}
  for (const [name, error] of Object.entries(errors)) {
    if (name !== key) {
      next[name] = error
    }
  }
  return next
}

interface SettingsActions {
  fieldErrors: SettingFieldErrors
  onChange: (key: string, value: string) => void
  onRestore: (key: string) => void
  onRestoreAll: (keys: readonly string[]) => void
}

function useSettingsActions(spaceSlug: string | undefined): SettingsActions {
  const change = useChangeSetting(spaceSlug)
  const restore = useRestoreSetting(spaceSlug)
  const restoreAll = useRestoreAllSettings(spaceSlug)
  const [fieldErrors, setFieldErrors] = useState<SettingFieldErrors>(NO_FIELD_ERRORS)

  return {
    fieldErrors,
    onChange: (key, value) => {
      setFieldErrors((previous) => withoutKey(previous, key))
      change.mutate(
        { key, value },
        {
          onError: (error) => {
            const fieldError = settingFieldError(key, error)
            if (fieldError !== undefined) {
              setFieldErrors((previous) => ({ ...previous, [key]: fieldError }))
            }
          },
        },
      )
    },
    onRestore: (key) => {
      restore.mutate(key)
    },
    onRestoreAll: (keys) => {
      restoreAll.mutate(keys)
    },
  }
}

function settingsStatus(
  settings: UseQueryResult<SpaceSetting[]>,
  access: UseQueryResult<SpaceAccess>,
): SettingsStatus {
  if (settings.isError) {
    return 'error'
  }
  if (settings.isPending || access.isPending) {
    return 'loading'
  }
  return 'ready'
}

function reloadSettings(queryClient: QueryClient, slug: string | undefined): void {
  if (slug === undefined) {
    return
  }
  void queryClient.invalidateQueries({ queryKey: settingsQueryKeys.space(slug) })
  void queryClient.invalidateQueries({ queryKey: settingsQueryKeys.access(slug) })
}

export function SettingsPage({ spaceSlug }: SettingsPageProps): ReactElement {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const settings = useSpaceSettings(spaceSlug)
  const access = useSpaceAccess(spaceSlug)
  const actions = useSettingsActions(spaceSlug)
  const groups = buildSettingsGroups(settings.data ?? [], t)
  const keys = overriddenSettingKeys(settings.data ?? [])

  return (
    <GeneralDefaultsView
      embedded
      groups={groups}
      status={settingsStatus(settings, access)}
      canEdit={canEditSpace(access.data)}
      fieldErrors={actions.fieldErrors}
      onChange={actions.onChange}
      onRestore={actions.onRestore}
      onRestoreAll={() => {
        actions.onRestoreAll(keys)
      }}
      onRetry={() => {
        reloadSettings(queryClient, spaceSlug)
      }}
    />
  )
}
