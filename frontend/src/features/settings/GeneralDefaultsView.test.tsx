import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '../../app/i18n'
import { ErrorCode, SettingGroup, SettingSource } from '../../shared/api/generated/enums'
import { renderWithProviders } from '../../test/providers'
import { installViewport } from '../../test/viewport'
import { GeneralDefaultsView } from './GeneralDefaultsView'
import type { GeneralDefaultsActions, GeneralDefaultsData, SettingsGroup } from './index'

const DESKTOP_WIDTH = 1024
const MOBILE_WIDTH = 375

const groups: readonly SettingsGroup[] = [
  {
    key: SettingGroup.GENERAL,
    title: 'Ogólne',
    rows: [
      {
        key: 'name',
        label: 'Nazwa przestrzeni',
        value: 'Produkcja',
        source: SettingSource.SPACE,
        kind: 'text',
      },
      {
        key: 'language',
        label: 'Język',
        value: 'Polski',
        source: SettingSource.INSTANCE,
        kind: 'select',
        options: [
          { value: 'pl', label: 'Polski' },
          { value: 'en', label: 'English' },
        ],
      },
      {
        key: 'timezone',
        label: 'Strefa czasowa',
        value: 'Europe/Warsaw',
        source: SettingSource.SPACE,
        overriddenValue: 'UTC',
        kind: 'select',
        options: [
          { value: 'UTC', label: 'UTC' },
          { value: 'Europe/Warsaw', label: 'Europe/Warsaw' },
        ],
      },
    ],
  },
  {
    key: SettingGroup.DEFAULTS,
    title: 'Domyślne dla nowych checków',
    rows: [
      {
        key: 'interval',
        label: 'Interwał',
        value: '300',
        source: SettingSource.SPACE,
        overriddenValue: '60 s',
        kind: 'number',
        unit: 's',
      },
      {
        key: 'timeout',
        label: 'Timeout',
        value: '10',
        source: SettingSource.INSTANCE,
        kind: 'number',
        unit: 's',
      },
    ],
  },
]

const inheritedGroups: readonly SettingsGroup[] = [
  {
    key: SettingGroup.GENERAL,
    title: 'Ogólne',
    rows: [
      {
        key: 'timeout',
        label: 'Timeout',
        value: '10',
        source: SettingSource.INSTANCE,
        kind: 'number',
        unit: 's',
      },
    ],
  },
]

type ViewOverrides = Partial<GeneralDefaultsData & GeneralDefaultsActions>

function renderView(overrides: ViewOverrides = {}): void {
  const data: GeneralDefaultsData = {
    groups,
    status: 'success',
    canEdit: true,
    ...overrides,
  }
  const actions: GeneralDefaultsActions = {
    onChange: vi.fn(),
    onRestore: vi.fn(),
    onRestoreAll: vi.fn(),
    onRetry: vi.fn(),
    ...overrides,
  }
  renderWithProviders(<GeneralDefaultsView data={data} actions={actions} />)
}

function rowFor(label: string): HTMLElement {
  const text = screen.getByText(label)
  const row = text.closest('tr')
  if (row === null) {
    throw new Error(`No table row for ${label}`)
  }
  return row
}

beforeEach(() => {
  installViewport(DESKTOP_WIDTH)
})

afterEach(async () => {
  cleanup()
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

describe('settings view tables and sources', () => {
  it('the settings view shows both tables with value and source', () => {
    renderView()

    expect(screen.getAllByRole('table')).toHaveLength(2)
    expect(screen.getByText('Ogólne')).toBeDefined()
    expect(screen.getByText('Domyślne dla nowych checków')).toBeDefined()
    const timezone = screen.getAllByLabelText<HTMLInputElement>('Strefa czasowa')
    expect(timezone.some((input) => input.value === 'Europe/Warsaw')).toBe(true)
    expect(screen.getByLabelText<HTMLInputElement>('Interwał').value).toBe('300')
    expect(screen.getAllByText('instancja').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Domyślne instancji').length).toBeGreaterThan(0)
  })

  it('an own value shows what it overrides and a restore action', () => {
    renderView()

    expect(screen.getByText('Własne · nadpisuje UTC')).toBeDefined()
    expect(screen.getByText('Własne · nadpisuje 60 s')).toBeDefined()
    expect(screen.getAllByRole('button', { name: 'Przywróć' })).toHaveLength(3)
  })
})

describe('settings restore actions', () => {
  it('restoring one value calls onRestore with its key', () => {
    const onRestore = vi.fn()
    renderView({ onRestore })

    const row = rowFor('Interwał')
    fireEvent.click(within(row).getByRole('button', { name: 'Przywróć' }))

    expect(onRestore).toHaveBeenCalledWith('interval')
    expect(onRestore).toHaveBeenCalledTimes(1)
  })

  it('restore all asks for confirmation before calling onRestoreAll', async () => {
    const onRestoreAll = vi.fn()
    renderView({ onRestoreAll })

    fireEvent.click(screen.getByRole('button', { name: 'Przywróć wszystkie' }))
    const dialog = await screen.findByRole('dialog')

    expect(onRestoreAll).not.toHaveBeenCalled()
    expect(dialog.getAttribute('aria-modal')).toBe('true')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Tak, przywróć wszystkie' }))
    expect(onRestoreAll).toHaveBeenCalledTimes(1)
  })

  it('restore all is hidden when nothing is overridden', () => {
    renderView({ groups: inheritedGroups })

    expect(screen.queryByRole('button', { name: 'Przywróć wszystkie' })).toBeNull()
  })
})

describe('settings editing', () => {
  it('editing a field calls onChange with the key and the new value', () => {
    const onChange = vi.fn()
    renderView({ onChange })
    const input = screen.getByLabelText('Nazwa przestrzeni')
    fireEvent.change(input, { target: { value: 'Staging' } })
    fireEvent.blur(input)

    expect(onChange).toHaveBeenCalledWith('name', 'Staging')
  })

  it('an out of plan error is shown next to the field with its bound', () => {
    const onChange = vi.fn()
    renderView({
      onChange,
      fieldErrors: { interval: { code: ErrorCode.OUT_OF_PLAN, bound: '300 s' } },
    })

    expect(screen.getByText('Poza planem: maksymalnie 300 s.')).toBeDefined()
    const input = screen.getByLabelText('Interwał')

    fireEvent.change(input, { target: { value: '400' } })
    fireEvent.blur(input)
    expect(onChange).toHaveBeenLastCalledWith('interval', '400')

    onChange.mockClear()
    fireEvent.change(input, { target: { value: '300' } })
    fireEvent.blur(input)
    expect(onChange).not.toHaveBeenCalled()
  })

  it('a read only view has no edit controls', () => {
    renderView({ canEdit: false })

    expect(screen.queryAllByRole('textbox')).toEqual([])
    expect(screen.queryAllByRole('combobox')).toEqual([])
    expect(screen.queryAllByRole('button')).toEqual([])
    expect(screen.getByText('Europe/Warsaw')).toBeDefined()
  })
})

describe('settings states', () => {
  it('on mobile a row opens a sheet to edit one field', async () => {
    installViewport(MOBILE_WIDTH)
    renderView()

    fireEvent.click(screen.getByRole('button', { name: 'Interwał' }))
    const dialog = await screen.findByRole('dialog', { name: 'Edytuj: Interwał' })

    expect(within(dialog).getByLabelText('Interwał')).toBeDefined()
    expect(within(dialog).queryByLabelText('Timeout')).toBeNull()
  })

  it('the settings view shows a skeleton while loading', () => {
    renderView({ status: 'pending', groups: [] })

    expect(screen.getByRole('status', { name: 'Ładowanie ustawień…' })).toBeDefined()
  })

  it('the settings view offers a retry after an error', () => {
    const onRetry = vi.fn()
    renderView({ status: 'error', groups: [], onRetry })

    fireEvent.click(screen.getByRole('button', { name: 'Spróbuj ponownie' }))

    expect(onRetry).toHaveBeenCalledTimes(1)
  })
})
