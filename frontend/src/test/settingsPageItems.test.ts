import { expect, it } from 'vitest'
import { SettingGroup, SettingSource, SettingType } from '../shared/api/generated/enums'
import { fetchSpaceSettings } from '../features/settings/api/settings.api'
import type { SpaceSetting } from '../features/settings/api/settings.schemas'
import { installMockApi } from './mockApi'

it('the settings list is read from the page items', async () => {
  // given a settings page whose settings are wrapped in items
  const items: SpaceSetting[] = [
    {
      key: 'timezone',
      planName: 'small',
      source: SettingSource.SPACE,
      value: 'Europe/Warsaw',
      type: SettingType.STRING,
      group: SettingGroup.GENERAL,
    },
    {
      key: 'check.interval',
      planName: 'small',
      source: SettingSource.PLAN,
      value: '60',
      type: SettingType.INTEGER,
      group: SettingGroup.DEFAULTS,
      unit: 's',
      min: 30,
      max: 300,
    },
  ]
  installMockApi(() => ({ status: 200, data: { items, nextCursor: 'ignored' } }))

  // when the client reads the settings of a space
  const result = await fetchSpaceSettings('alpha', new AbortController().signal)

  // then only the items of the page are returned
  expect(result).toEqual(items)
})
