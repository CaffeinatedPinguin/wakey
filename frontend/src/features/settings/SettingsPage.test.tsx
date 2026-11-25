import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { appConfig } from '../../shared/config'
import { signedInUser } from '../../test/data'
import { installMockApi } from '../../test/mockApi'
import type { MockReply, MockRequest } from '../../test/mockApi'
import { renderApp } from '../../test/renderApp'
import { installViewport } from '../../test/viewport'
import i18n from '../../app/i18n'
import type { Me } from '../auth'

const ME_URL = `${appConfig.apiBaseUrl}/me`
const SPACES_URL = `${appConfig.apiBaseUrl}/spaces`
const SETTINGS_URL = `${SPACES_URL}/alpha/settings`
const DESKTOP_WIDTH = 1024
const OUT_OF_PLAN_BOUND = 300

type Source = 'SPACE' | 'PLAN' | 'INSTANCE' | 'DEFAULT'
type Role = 'EDITOR' | 'VIEWER' | null

interface SettingFixture {
  key: string
  planName: string
  source: Source
  value: string
}

interface SpaceFixture {
  slug: string
  name: string
  admin: boolean
  role: Role
  version: number
}

interface ApiState {
  me: Me
  role: Role
  settings: SettingFixture[]
  settingsFetches: number
  writes: { key: string; value: string }[]
  deletes: string[]
}

function createSettings(): SettingFixture[] {
  return [
    { key: 'timezone', planName: 'small', source: 'SPACE', value: 'Europe/Warsaw' },
    { key: 'language', planName: 'small', source: 'INSTANCE', value: 'pl' },
    { key: 'check.interval', planName: 'small', source: 'PLAN', value: '60' },
    { key: 'check.timeout', planName: 'small', source: 'DEFAULT', value: '10' },
    { key: 'check.retries', planName: 'small', source: 'SPACE', value: '1' },
  ]
}

function createApiState(): ApiState {
  return {
    me: { ...signedInUser },
    role: 'EDITOR',
    settings: createSettings(),
    settingsFetches: 0,
    writes: [],
    deletes: [],
  }
}

let api = createApiState()

function spaceFixture(): SpaceFixture {
  return {
    slug: 'alpha',
    name: 'Alpha',
    admin: api.me.admin,
    role: api.role,
    version: 1,
  }
}

function valueFrom(body: string | undefined): string {
  if (body === undefined) {
    return ''
  }
  const parsed = JSON.parse(body) as { value?: unknown }
  return typeof parsed.value === 'string' ? parsed.value : ''
}

function handlePut(key: string, body: string | undefined): MockReply {
  const value = valueFrom(body)
  api.writes.push({ key, value })
  if (key === 'check.interval' && Number(value) > OUT_OF_PLAN_BOUND) {
    return {
      status: 400,
      data: {
        code: 'out_of_plan',
        params: { bound: OUT_OF_PLAN_BOUND, key, planName: 'small' },
      },
    }
  }
  api.settings = api.settings.map((setting) =>
    setting.key === key ? { ...setting, source: 'SPACE', value } : setting,
  )
  return { status: 204 }
}

function handleDelete(key: string): MockReply {
  api.deletes.push(key)
  api.settings = api.settings.map((setting) =>
    setting.key === key ? { ...setting, source: 'PLAN', value: '30' } : setting,
  )
  return { status: 204 }
}

function handleSettings(
  key: string | undefined,
  method: string,
  body: string | undefined,
): MockReply {
  if (method === 'get') {
    api.settingsFetches += 1
    return { status: 200, data: api.settings }
  }
  if (key === undefined) {
    return { status: 404, data: { code: 'not_found' } }
  }
  if (method === 'put') {
    return handlePut(key, body)
  }
  if (method === 'delete') {
    return handleDelete(key)
  }
  return { status: 404, data: { code: 'not_found' } }
}

function settingKey(url: string): string | undefined {
  const prefix = `${SETTINGS_URL}/`
  return url.startsWith(prefix) ? decodeURIComponent(url.slice(prefix.length)) : undefined
}

function handleRequest(request: MockRequest): MockReply {
  const url = request.url.split('?')[0] ?? ''
  const method = request.method.toLowerCase()
  if (request.url === ME_URL) {
    return { status: 200, data: api.me }
  }
  if (url === SPACES_URL) {
    return { status: 200, data: { items: [spaceFixture()], nextCursor: null } }
  }
  if (url === `${SPACES_URL}/alpha`) {
    return { status: 200, data: spaceFixture() }
  }
  if (url === SETTINGS_URL || url.startsWith(`${SETTINGS_URL}/`)) {
    return handleSettings(settingKey(url), method, request.body)
  }
  return { status: 404, data: { code: 'not_found' } }
}

function rowFor(label: string): HTMLElement {
  const row = screen.getByText(label).closest('tr')
  if (row === null) {
    throw new Error(`No table row for ${label}`)
  }
  return row
}

beforeEach(() => {
  api = createApiState()
  installMockApi(handleRequest)
  installViewport(DESKTOP_WIDTH)
})

afterEach(async () => {
  cleanup()
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

describe('settings page data', () => {
  it('the settings page shows the effective values with their sources from the API', async () => {
    renderApp('/settings?space=alpha')

    expect(await screen.findByDisplayValue('Europe/Warsaw')).toBeDefined()
    expect(screen.getByDisplayValue('60')).toBeDefined()
    expect(screen.getByText('instancja')).toBeDefined()
    expect(screen.getByText('plan')).toBeDefined()
    expect(screen.getByText('domyślne')).toBeDefined()
    expect(screen.getAllByText('Własne')).toHaveLength(2)
  })

  it('the settings page keeps the space in the URL', async () => {
    const { router } = renderApp('/settings?space=alpha')
    await screen.findByDisplayValue('Europe/Warsaw')

    fireEvent.change(screen.getByLabelText('Interwał'), { target: { value: '45' } })

    await waitFor(() => {
      expect(api.writes).toEqual([{ key: 'check.interval', value: '45' }])
    })
    expect(router.state.location.search).toEqual({ space: 'alpha' })
  })
})

describe('settings changes', () => {
  it('changing a value sends it for the current space and refreshes the list', async () => {
    renderApp('/settings?space=alpha')
    const interval = await screen.findByLabelText('Interwał')

    fireEvent.change(interval, { target: { value: '45' } })

    await waitFor(() => {
      expect(api.writes).toEqual([{ key: 'check.interval', value: '45' }])
    })
    await waitFor(() => {
      expect(api.settingsFetches).toBeGreaterThanOrEqual(2)
    })
    expect(await screen.findByDisplayValue('45')).toBeDefined()
    expect(screen.getAllByText('Własne')).toHaveLength(3)
  })

  it('a value outside the plan shows the bound next to the field', async () => {
    renderApp('/settings?space=alpha')
    const interval = await screen.findByLabelText('Interwał')

    fireEvent.change(interval, { target: { value: '400' } })

    expect(await screen.findByText('Poza planem: maksymalnie 300 s.')).toBeDefined()
    expect(api.writes).toEqual([{ key: 'check.interval', value: '400' }])
  })

  it('the settings page reads the bound from the error params', async () => {
    renderApp('/settings?space=alpha')
    const interval = await screen.findByLabelText('Interwał')

    fireEvent.change(interval, { target: { value: '400' } })

    expect(await screen.findByText('Poza planem: maksymalnie 300 s.')).toBeDefined()
  })
})

describe('settings restore actions', () => {
  it('restoring a value resets it through the API', async () => {
    renderApp('/settings?space=alpha')
    await screen.findByDisplayValue('Europe/Warsaw')

    const row = rowFor('Strefa czasowa')
    fireEvent.click(within(row).getByRole('button', { name: 'Przywróć' }))

    await waitFor(() => {
      expect(api.deletes).toEqual(['timezone'])
    })
  })

  it('restore all resets every overridden value', async () => {
    renderApp('/settings?space=alpha')
    await screen.findByDisplayValue('Europe/Warsaw')

    fireEvent.click(screen.getByRole('button', { name: 'Przywróć wszystkie' }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Tak, przywróć wszystkie' }))

    await waitFor(() => {
      expect(api.deletes).toEqual(['timezone', 'check.retries'])
    })
  })
})

describe('settings permissions', () => {
  it('a viewer sees the settings without edit controls', async () => {
    api.me = { ...signedInUser, admin: false }
    api.role = 'VIEWER'
    renderApp('/settings?space=alpha')

    expect(await screen.findByText('Europe/Warsaw')).toBeDefined()
    expect(screen.queryByLabelText('Interwał')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Przywróć' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Przywróć wszystkie' })).toBeNull()
  })
})
