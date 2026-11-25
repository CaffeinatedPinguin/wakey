import type { TFunction } from 'i18next'
import type {
  SettingFieldError,
  SettingKind,
  SettingOption,
  SettingRowData,
  SettingSource,
  SettingsGroup,
} from './types'
import { ApiError } from '../../shared/api/problem'
import type { SpaceAccess, SpaceSetting } from './api/settings.schemas'

type SettingsGroupKind = 'general' | 'defaults'

interface SettingDefinition {
  key: string
  labelKey: string
  group: SettingsGroupKind
  kind: SettingKind
  unit?: string
  options?: readonly SettingOption[]
}

const LANGUAGE_OPTIONS: readonly SettingOption[] = [
  { value: 'pl', label: 'Polski' },
  { value: 'en', label: 'English' },
]

const SETTING_DEFINITIONS: readonly SettingDefinition[] = [
  { key: 'timezone', labelKey: 'settings.keys.timezone', group: 'general', kind: 'text' },
  {
    key: 'language',
    labelKey: 'settings.keys.language',
    group: 'general',
    kind: 'select',
    options: LANGUAGE_OPTIONS,
  },
  {
    key: 'check.interval',
    labelKey: 'settings.keys.interval',
    group: 'defaults',
    kind: 'number',
    unit: 's',
  },
  {
    key: 'check.timeout',
    labelKey: 'settings.keys.timeout',
    group: 'defaults',
    kind: 'number',
    unit: 's',
  },
  {
    key: 'check.retries',
    labelKey: 'settings.keys.retries',
    group: 'defaults',
    kind: 'number',
  },
]

const SOURCES: Record<SpaceSetting['source'], SettingSource> = {
  SPACE: 'space',
  PLAN: 'plan',
  INSTANCE: 'instance',
  DEFAULT: 'default',
}

const BOUND_UNITS: Record<string, string> = {
  'check.interval': 's',
  'check.timeout': 's',
}

function toRow(definition: SettingDefinition, setting: SpaceSetting, t: TFunction): SettingRowData {
  const row: SettingRowData = {
    key: definition.key,
    label: t(definition.labelKey),
    value: setting.value,
    source: SOURCES[setting.source],
    kind: definition.kind,
  }
  return {
    ...row,
    ...(definition.unit === undefined ? {} : { unit: definition.unit }),
    ...(definition.options === undefined ? {} : { options: definition.options }),
  }
}

function rowsFor(
  group: SettingsGroupKind,
  settings: readonly SpaceSetting[],
  t: TFunction,
): SettingRowData[] {
  return SETTING_DEFINITIONS.filter((definition) => definition.group === group).flatMap(
    (definition) => {
      const setting = settings.find((item) => item.key === definition.key)
      return setting === undefined ? [] : [toRow(definition, setting, t)]
    },
  )
}

export function buildSettingsGroups(
  settings: readonly SpaceSetting[],
  t: TFunction,
): SettingsGroup[] {
  return [
    {
      key: 'general',
      title: t('settings.groups.general'),
      rows: rowsFor('general', settings, t),
    },
    {
      key: 'defaults',
      title: t('settings.groups.defaults'),
      rows: rowsFor('defaults', settings, t),
    },
  ]
}

export function overriddenSettingKeys(settings: readonly SpaceSetting[]): string[] {
  return settings.filter((setting) => setting.source === 'SPACE').map((setting) => setting.key)
}

export function canEditSpace(access: SpaceAccess | undefined): boolean {
  if (access === undefined) {
    return false
  }
  return access.admin || access.role === 'EDITOR'
}

function boundText(key: string, bound: unknown): string | undefined {
  if (typeof bound === 'number') {
    const unit = BOUND_UNITS[key]
    return unit === undefined ? String(bound) : `${String(bound)} ${unit}`
  }
  if (typeof bound === 'string') {
    const unit = BOUND_UNITS[key]
    return unit !== undefined && /^\d+$/.test(bound) ? `${bound} ${unit}` : bound
  }
  return undefined
}

export function settingFieldError(key: string, error: unknown): SettingFieldError | undefined {
  if (!(error instanceof ApiError)) {
    return undefined
  }
  if (error.code === 'out_of_plan') {
    const bound = boundText(key, error.params.bound)
    return { code: 'out_of_plan', ...(bound === undefined ? {} : { bound }) }
  }
  if (error.code === 'validation_failed' || error.status === 403) {
    return { code: 'invalid' }
  }
  return undefined
}
