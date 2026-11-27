import { expect, it } from 'vitest'
import i18n from '../app/i18n'
import { buildSettingsGroups } from '../features/settings/settingsModel'
import type { SpaceSetting } from '../features/settings/api/settings.schemas'
import { SettingGroup } from '../shared/api/generated/enums'
import { setting } from './settingsData'

function settingsForEveryGroup(): SpaceSetting[] {
  return Object.values(SettingGroup).map((group) => setting({ key: `custom.${group}`, group }))
}

it('every setting group from the contract is rendered in contract order', () => {
  // given one setting for every group the contract defines
  const settings = settingsForEveryGroup()

  // when the groups are built from the API descriptor
  const groups = buildSettingsGroups(settings, i18n.t)

  // then every contract group is rendered, in contract order, and under a translated key
  expect(groups.map((group) => group.key)).toEqual(Object.values(SettingGroup))
  for (const group of groups) {
    expect(group.rows).toHaveLength(1)
    expect(group.title).toBe(i18n.t(`settings.groups.${group.key}`))
    expect(group.title).not.toBe(group.key)
  }
})
