import { Language, SettingGroup, SettingSource, SettingType } from '../shared/api/generated/enums'
import type { SpaceSetting } from '../features/settings/api/settings.schemas'

export function setting(overrides: Partial<SpaceSetting> & { key: string }): SpaceSetting {
  return {
    planName: 'small',
    source: SettingSource.INSTANCE,
    value: '',
    type: SettingType.STRING,
    group: SettingGroup.GENERAL,
    ...overrides,
  }
}

function timezoneSetting(): SpaceSetting {
  return setting({ key: 'timezone', source: SettingSource.SPACE, value: 'Europe/Warsaw' })
}

function languageSetting(): SpaceSetting {
  return setting({
    key: 'language',
    source: SettingSource.INSTANCE,
    value: Language.PL,
    type: SettingType.ENUM,
    options: [Language.PL, Language.EN],
  })
}

function intervalSetting(): SpaceSetting {
  return setting({
    key: 'check.interval',
    source: SettingSource.PLAN,
    value: '60',
    type: SettingType.INTEGER,
    group: SettingGroup.DEFAULTS,
    unit: 's',
    min: 30,
    max: 300,
  })
}

function timeoutSetting(): SpaceSetting {
  return setting({
    key: 'check.timeout',
    source: SettingSource.INSTANCE,
    value: '10',
    type: SettingType.INTEGER,
    group: SettingGroup.DEFAULTS,
    unit: 's',
    min: 1,
    max: 60,
  })
}

function retriesSetting(): SpaceSetting {
  return setting({
    key: 'check.retries',
    source: SettingSource.SPACE,
    value: '1',
    type: SettingType.INTEGER,
    group: SettingGroup.DEFAULTS,
    min: 0,
    max: 5,
  })
}

export function createSettings(): SpaceSetting[] {
  return [
    timezoneSetting(),
    languageSetting(),
    intervalSetting(),
    timeoutSetting(),
    retriesSetting(),
  ]
}
