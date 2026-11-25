export type SettingSource = 'instance' | 'plan' | 'space' | 'default'

export type SettingKind = 'text' | 'select' | 'number'

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
  readOnlyOnMobile?: boolean
}

export interface SettingsGroup {
  key: string
  title: string
  rows: readonly SettingRowData[]
}

export interface SettingFieldError {
  code: string
  bound?: string
}

export type SettingFieldErrors = Readonly<Record<string, SettingFieldError>>

export type SettingsStatus = 'loading' | 'ready' | 'error'

export interface GeneralDefaultsViewProps {
  groups: readonly SettingsGroup[]
  status: SettingsStatus
  canEdit: boolean
  onChange: (key: string, value: string) => void
  onRestore: (key: string) => void
  onRestoreAll: () => void
  onRetry: () => void
  fieldErrors?: SettingFieldErrors
  embedded?: boolean
}
