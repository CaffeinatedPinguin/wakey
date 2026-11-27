import { Alert } from '@mantine/core'
import type { QueryStatus, UseQueryResult } from '@tanstack/react-query'
import type { ReactElement } from 'react'
import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  useChangeSetting,
  useRestoreAllSettings,
  useRestoreSetting,
  useSpaceSettings,
} from './api/settings.queries'
import type { RestoreHandlers } from './api/settings.queries'
import type { SpaceAccess, SpaceSetting } from './api/settings.schemas'
import { apiErrorMessage } from '../../shared/api/formErrors'
import { GeneralDefaultsView } from './GeneralDefaultsView'
import { buildSettingsGroups, canEditSpace, settingFieldError } from './settingsModel'
import type { SettingFieldError, SettingFieldErrors } from './types'

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
  restoreError: string | null
  restoring: boolean
  onChange: (key: string, value: string) => void
  onRestore: (key: string) => void
  onRestoreAll: () => void
}

interface SettingChanges {
  fieldErrors: SettingFieldErrors
  onChange: (key: string, value: string) => void
}

function unitFor(settings: readonly SpaceSetting[], key: string): string | undefined {
  return settings.find((setting) => setting.key === key)?.unit ?? undefined
}

function useSettingChanges(
  spaceSlug: string | undefined,
  settings: readonly SpaceSetting[],
): SettingChanges {
  const change = useChangeSetting(spaceSlug)
  const [fieldErrors, setFieldErrors] = useState<SettingFieldErrors>(NO_FIELD_ERRORS)
  const onChange = (key: string, value: string): void => {
    setFieldErrors((previous) => withoutKey(previous, key))
    change.mutate(
      { key, value },
      {
        onError: (error) => {
          const fieldError = settingFieldError(error, unitFor(settings, key))
          if (fieldError !== undefined) {
            setFieldErrors((previous) => ({ ...previous, [key]: fieldError }))
          }
        },
      },
    )
  }
  return { fieldErrors, onChange }
}

interface SettingRestores {
  restoreError: string | null
  restoring: boolean
  onRestore: (key: string) => void
  onRestoreAll: () => void
}

function useSettingRestores(spaceSlug: string | undefined): SettingRestores {
  const { t } = useTranslation()
  const [restoreError, setRestoreError] = useState<string | null>(null)
  const [restoring, setRestoring] = useState(false)
  const restoringRef = useRef(false)
  const handlers: RestoreHandlers = {
    onError: (error) => {
      setRestoreError(apiErrorMessage(t, error))
    },
    onSettled: () => {
      restoringRef.current = false
      setRestoring(false)
    },
  }
  const restore = useRestoreSetting(spaceSlug, handlers)
  const restoreAll = useRestoreAllSettings(spaceSlug, handlers)
  const beginRestore = (): boolean => {
    if (restoringRef.current) {
      return false
    }
    restoringRef.current = true
    setRestoring(true)
    setRestoreError(null)
    return true
  }
  const onRestore = (key: string): void => {
    if (beginRestore()) {
      restore.mutate(key)
    }
  }
  const onRestoreAll = (): void => {
    if (beginRestore()) {
      restoreAll.mutate()
    }
  }
  return { restoreError, restoring, onRestore, onRestoreAll }
}

function useSettingsActions(
  spaceSlug: string | undefined,
  settings: readonly SpaceSetting[],
): SettingsActions {
  const changes = useSettingChanges(spaceSlug, settings)
  const restores = useSettingRestores(spaceSlug)
  return { ...changes, ...restores }
}

function settingsStatus(
  settings: UseQueryResult<SpaceSetting[]>,
  accessLoading: boolean,
): QueryStatus {
  return settings.status === 'success' && accessLoading ? 'pending' : settings.status
}

function RestoreErrorAlert({ message }: { message: string | null }): ReactElement | null {
  if (message === null) {
    return null
  }
  return (
    <Alert color="danger" mb="sm">
      {message}
    </Alert>
  )
}

export function SettingsPage({
  spaceSlug,
  access,
  accessLoading,
  isAdmin,
}: SettingsPageProps): ReactElement {
  const { t } = useTranslation()
  const settings = useSpaceSettings(spaceSlug)
  const actions = useSettingsActions(spaceSlug, settings.data ?? [])
  const groups = buildSettingsGroups(settings.data ?? [], t)

  return (
    <>
      <RestoreErrorAlert message={actions.restoreError} />
      <GeneralDefaultsView
        data={{
          groups,
          status: settingsStatus(settings, accessLoading),
          canEdit: canEditSpace(access, isAdmin),
          fieldErrors: actions.fieldErrors,
          embedded: true,
        }}
        actions={{
          onChange: actions.onChange,
          onRestore: actions.onRestore,
          onRestoreAll: actions.onRestoreAll,
          onRetry: () => {
            void settings.refetch()
          },
          restoring: actions.restoring,
        }}
      />
    </>
  )
}
