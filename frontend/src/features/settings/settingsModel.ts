import type { TFunction } from 'i18next'
import { SettingFieldClientErrorCode, SettingKind } from './types'
import type { SettingFieldError, SettingOption, SettingRowData, SettingsGroup } from './types'
import { ApiException } from '../../shared/api/apiException'
import { HttpStatus } from '../../shared/api/httpStatus'
import { ErrorCode, SettingGroup, SettingType, SpaceRole } from '../../shared/api/generated/enums'
import type { SpaceAccess, SpaceSetting } from './api/settings.schemas'

// The descriptor type chooses the control; the contract values stay the only
// source of truth for what a setting is.
const KIND_BY_TYPE: Record<SettingType, SettingKind> = {
  [SettingType.STRING]: SettingKind.TEXT,
  [SettingType.INTEGER]: SettingKind.NUMBER,
  [SettingType.ENUM]: SettingKind.SELECT,
}

// Only labels live in i18n; a key without a translation is shown as it arrives.
function labelFor(key: string, t: TFunction): string {
  return t(`settings.keys.${key}`, { defaultValue: key })
}

function optionsFor(setting: SpaceSetting, t: TFunction): readonly SettingOption[] {
  const values = setting.options ?? []
  return values.map((value) => ({ value, label: t(`settings.languages.${value}`) }))
}

function toRow(setting: SpaceSetting, t: TFunction): SettingRowData {
  const kind = KIND_BY_TYPE[setting.type]
  const unit = setting.unit ?? undefined
  const min = setting.min ?? undefined
  const max = setting.max ?? undefined
  return {
    key: setting.key,
    label: labelFor(setting.key, t),
    value: setting.value,
    source: setting.source,
    kind,
    ...(unit === undefined ? {} : { unit }),
    ...(kind === SettingKind.SELECT ? { options: optionsFor(setting, t) } : {}),
    ...(min === undefined ? {} : { min }),
    ...(max === undefined ? {} : { max }),
  }
}

function rowsFor(
  group: SettingGroup,
  settings: readonly SpaceSetting[],
  t: TFunction,
): SettingRowData[] {
  return settings.filter((setting) => setting.group === group).map((setting) => toRow(setting, t))
}

export function buildSettingsGroups(
  settings: readonly SpaceSetting[],
  t: TFunction,
): SettingsGroup[] {
  return Object.values(SettingGroup).map((group) => ({
    key: group,
    title: t(`settings.groups.${group}`),
    rows: rowsFor(group, settings, t),
  }))
}

export function canEditSpace(access: SpaceAccess | undefined, isAdmin: boolean): boolean {
  if (access === undefined) {
    return false
  }
  return isAdmin || access.role === SpaceRole.EDITOR
}

function boundText(bound: unknown, unit: string | undefined): string | undefined {
  if (typeof bound === 'number') {
    return unit === undefined ? String(bound) : `${String(bound)} ${unit}`
  }
  if (typeof bound === 'string') {
    return unit !== undefined && /^\d+$/.test(bound) ? `${bound} ${unit}` : bound
  }
  return undefined
}

export function settingFieldError(
  error: unknown,
  unit: string | undefined,
): SettingFieldError | undefined {
  if (!(error instanceof ApiException)) {
    return undefined
  }
  if (error.code === ErrorCode.OUT_OF_PLAN) {
    const bound = boundText(error.params.bound, unit)
    return { code: ErrorCode.OUT_OF_PLAN, ...(bound === undefined ? {} : { bound }) }
  }
  if (error.code === ErrorCode.VALIDATION_FAILED || error.status === HttpStatus.FORBIDDEN) {
    return { code: ErrorCode.VALIDATION_FAILED }
  }
  return { code: SettingFieldClientErrorCode.SAVE_FAILED }
}
