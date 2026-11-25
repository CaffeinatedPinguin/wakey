import type { ReactElement } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  useChangeSetting,
  useRestoreAllSettings,
  useRestoreSetting,
  useSpaceSettings,
} from './api/settings.queries'
import type { SpaceAccess } from './api/settings.schemas'
import { GeneralDefaultsView } from './GeneralDefaultsView'
import {
  buildSettingsGroups,
  canEditSpace,
  overriddenSettingKeys,
  settingFieldError,
} from './settingsModel'
import type { SettingFieldError, SettingFieldErrors, SettingsStatus } from './types'

export interface SettingsPageProps {
  spaceSlug: string | undefined
  access: SpaceAccess | undefined
  isAdmin: boolean
  accessLoading: boolean
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
  settings: ReturnType<typeof useSpaceSettings>,
  accessLoading: boolean,
): SettingsStatus {
  if (settings.isError) {
    return 'error'
  }
  if (settings.isPending || accessLoading) {
    return 'loading'
  }
  return 'ready'
}

export function SettingsPage({
  spaceSlug,
  access,
  accessLoading,
  isAdmin,
}: SettingsPageProps): ReactElement {
  const { t } = useTranslation()
  const settings = useSpaceSettings(spaceSlug)
  const actions = useSettingsActions(spaceSlug)
  const groups = buildSettingsGroups(settings.data ?? [], t)
  const keys = overriddenSettingKeys(settings.data ?? [])

  return (
    <GeneralDefaultsView
      embedded
      groups={groups}
      status={settingsStatus(settings, accessLoading)}
      canEdit={canEditSpace(access, isAdmin)}
      fieldErrors={actions.fieldErrors}
      onChange={actions.onChange}
      onRestore={actions.onRestore}
      onRestoreAll={() => {
        actions.onRestoreAll(keys)
      }}
      onRetry={() => {
        void settings.refetch()
      }}
    />
  )
}
