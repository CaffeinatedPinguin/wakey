import { cleanup, screen } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import i18n from '../../app/i18n'
import {
  Language,
  SettingGroup,
  SettingSource,
  SettingType,
} from '../../shared/api/generated/enums'
import { renderWithProviders } from '../../test/providers'
import { setting } from '../../test/settingsData'
import { installViewport } from '../../test/viewport'
import { GeneralDefaultsView } from './GeneralDefaultsView'
import type { SpaceSetting } from './api/settings.schemas'
import { buildSettingsGroups } from './settingsModel'
import type { GeneralDefaultsActions, GeneralDefaultsData, SettingRowData } from './types'

const DESKTOP_WIDTH = 1024

function rowsFor(settings: readonly SpaceSetting[]): SettingRowData[] {
  return buildSettingsGroups(settings, i18n.t).flatMap((group) => group.rows)
}

function rowFor(settings: readonly SpaceSetting[], key: string): SettingRowData {
  const row = rowsFor(settings).find((item) => item.key === key)
  if (row === undefined) {
    throw new Error(`No row for ${key}`)
  }
  return row
}

function renderSettings(settings: readonly SpaceSetting[]): void {
  const data: GeneralDefaultsData = {
    groups: buildSettingsGroups(settings, i18n.t),
    status: 'success',
    canEdit: true,
  }
  const actions: GeneralDefaultsActions = {
    onChange: vi.fn(),
    onRestore: vi.fn(),
    onRestoreAll: vi.fn(),
    onRetry: vi.fn(),
  }
  renderWithProviders(<GeneralDefaultsView data={data} actions={actions} />)
}

beforeEach(() => {
  installViewport(DESKTOP_WIDTH)
})

afterEach(async () => {
  cleanup()
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

it('a setting is rendered from its descriptor', () => {
  // given settings that carry their own type, unit, bounds and group
  const settings = [
    setting({ key: 'timezone', value: 'UTC', type: SettingType.STRING }),
    setting({
      key: 'check.interval',
      value: '60',
      type: SettingType.INTEGER,
      group: SettingGroup.DEFAULTS,
      unit: 's',
      min: 30,
      max: 300,
    }),
    setting({
      key: 'language',
      value: Language.PL,
      type: SettingType.ENUM,
      options: [Language.PL, Language.EN],
    }),
  ]

  // when the settings are rendered
  renderSettings(settings)

  // then each control and its context come from the descriptor
  expect(screen.getByRole('textbox', { name: 'Strefa czasowa' })).toBeDefined()
  expect(screen.getByRole('textbox', { name: 'Interwał' })).toBeDefined()
  expect(screen.getByRole('combobox', { name: 'Język' })).toBeDefined()
  expect(screen.getByText('Ogólne')).toBeDefined()
  expect(screen.getByText('Domyślne dla nowych checków')).toBeDefined()
  expect(screen.getByText('s')).toBeDefined()
})

it('an unknown setting key from the server is still shown', () => {
  // given a setting key that has no translation
  const settings = [
    setting({
      key: 'custom.limit',
      value: '5',
      type: SettingType.INTEGER,
      group: SettingGroup.DEFAULTS,
    }),
  ]

  // when the settings are rendered
  renderSettings(settings)

  // then the key itself is the label and the value is shown
  expect(screen.getByRole('textbox', { name: 'custom.limit' })).toBeDefined()
  expect(screen.getByDisplayValue('5')).toBeDefined()
  expect(screen.getByText('custom.limit')).toBeDefined()
})

it('the language options come from the contract', () => {
  // given an enum descriptor whose options arrive in contract order
  const settings = [
    setting({
      key: 'language',
      value: Language.EN,
      type: SettingType.ENUM,
      options: [Language.EN, Language.PL],
    }),
  ]

  // when the row is built from the descriptor
  const language = rowFor(settings, 'language')

  // then the options keep the contract values and take labels from i18n
  expect(language.options).toEqual([
    { value: Language.EN, label: 'English' },
    { value: Language.PL, label: 'Polski' },
  ])
})

it('the value source uses the contract values', () => {
  // given settings from every source the contract defines
  const settings = [
    setting({ key: 'timezone', source: SettingSource.INSTANCE }),
    setting({ key: 'check.interval', source: SettingSource.PLAN }),
    setting({ key: 'language', source: SettingSource.SPACE }),
  ]

  // when the rows are built from the response
  // then each row carries the generated source value
  expect(rowFor(settings, 'timezone').source).toBe(SettingSource.INSTANCE)
  expect(rowFor(settings, 'check.interval').source).toBe(SettingSource.PLAN)
  expect(rowFor(settings, 'language').source).toBe(SettingSource.SPACE)
})
