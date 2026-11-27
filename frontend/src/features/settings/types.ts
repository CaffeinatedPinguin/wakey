import type { QueryStatus } from '@tanstack/react-query'
import type { ErrorCode, SettingSource } from '../../shared/api/generated/enums'

export type { SettingSource } from '../../shared/api/generated/enums'

export const SettingKind = {
  TEXT: 'text',
  SELECT: 'select',
  NUMBER: 'number',
} as const
export type SettingKind = (typeof SettingKind)[keyof typeof SettingKind]

export interface SettingOption {
  value: string
  label: string
}

export interface SettingRowData {
  key: string
  label: string
  value: string
  source: SettingSource
  overriddenValue?: string
  kind: SettingKind
  options?: readonly SettingOption[]
  unit?: string
  min?: number
  max?: number
  readOnlyOnMobile?: boolean
}

export interface SettingsGroup {
  key: string
  title: string
  rows: readonly SettingRowData[]
}

// Only the code the client adds to the contract vocabulary for a setting field;
// the contract codes stay in ErrorCode.
export const SettingFieldClientErrorCode = {
  SAVE_FAILED: 'save_failed',
} as const
export type SettingFieldClientErrorCode =
  (typeof SettingFieldClientErrorCode)[keyof typeof SettingFieldClientErrorCode]

export type SettingFieldErrorCode = ErrorCode | SettingFieldClientErrorCode

export interface SettingFieldError {
  code: SettingFieldErrorCode
  bound?: string
}

export type SettingFieldErrors = Readonly<Record<string, SettingFieldError>>

export interface GeneralDefaultsData {
  groups: readonly SettingsGroup[]
  status: QueryStatus
  canEdit: boolean
  fieldErrors?: SettingFieldErrors
  embedded?: boolean
}

export interface GeneralDefaultsActions {
  onChange: (key: string, value: string) => void
  onRestore: (key: string) => void
  onRestoreAll: () => void
  onRetry: () => void
  restoring?: boolean
}

export interface GeneralDefaultsViewProps {
  data: GeneralDefaultsData
  actions: GeneralDefaultsActions
}
