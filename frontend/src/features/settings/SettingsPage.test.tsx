import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { appConfig } from '../../shared/config'
import { ErrorCode, SettingSource } from '../../shared/api/generated/enums'
import type { SpaceRole } from '../../shared/api/generated/enums'
import { signedInUser } from '../../test/data'
import { installMockApi } from '../../test/mockApi'
import type { MockReply, MockRequest } from '../../test/mockApi'
import { renderApp } from '../../test/renderApp'
import { createSettings } from '../../test/settingsData'
import { installViewport } from '../../test/viewport'
import i18n from '../../app/i18n'
import type { Me } from '../auth'
import type { SpaceSetting } from './api/settings.schemas'
const SPACES_URL = `${appConfig.apiBaseUrl}/spaces`
const SETTINGS_URL = `${SPACES_URL}/alpha/settings`
const DESKTOP_WIDTH = 1024
interface SpaceFixture {
  slug: string
  name: string
  admin: boolean
  role: SpaceRole | null
  version: number
}

interface ApiState {
  me: Me
  role: SpaceRole | null
  settings: SpaceSetting[]
  settingsFetches: number
  writes: { key: string; value: string }[]
}

function createApiState(): ApiState {
  return {
    me: { ...signedInUser },
    role: 'EDITOR',
    settings: createSettings(),
    settingsFetches: 0,
    writes: [],
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
  if (key === 'timezone' && value === 'Not/AZone') {
    return {
      status: 400,
      data: { code: ErrorCode.VALIDATION_FAILED, params: {}, errors: [] },
    }
  }
  if (key === 'check.interval' && Number(value) > 300) {
    return {
      status: 400,
      data: {
        code: ErrorCode.OUT_OF_PLAN,
        params: { bound: 300, key, planName: 'small' },
      },
    }
  }
  api.settings = api.settings.map((setting) =>
    setting.key === key ? { ...setting, source: SettingSource.SPACE, value } : setting,
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
    return { status: 200, data: { items: api.settings, nextCursor: null } }
  }
  if (key === undefined) {
    return { status: 404, data: { code: 'not_found' } }
  }
  if (method === 'put') {
    return handlePut(key, body)
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
  if (request.url === `${appConfig.apiBaseUrl}/me`) {
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

function changeAndBlur(label: string, value: string): void {
  const input = screen.getByLabelText(label)
  fireEvent.change(input, { target: { value } })
  fireEvent.blur(input)
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
    expect(screen.getAllByText('instancja').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Plan').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Własne')).toHaveLength(2)
  })

  it('the settings page keeps the space in the URL', async () => {
    const { router } = renderApp('/settings?space=alpha')
    await screen.findByDisplayValue('Europe/Warsaw')

    changeAndBlur('Interwał', '45')

    await waitFor(() => {
      expect(api.writes).toEqual([{ key: 'check.interval', value: '45' }])
    })
    expect(router.state.location.search).toEqual({ space: 'alpha' })
  })
})

describe('settings changes', () => {
  it('changing a value sends it for the current space and refreshes the list', async () => {
    renderApp('/settings?space=alpha')
    await screen.findByLabelText('Interwał')

    changeAndBlur('Interwał', '45')

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
    await screen.findByLabelText('Interwał')

    changeAndBlur('Interwał', '400')

    expect(await screen.findByText('Poza planem: maksymalnie 300 s.')).toBeDefined()
    expect(api.writes).toEqual([{ key: 'check.interval', value: '400' }])
  })

  it('the settings page reads the bound from the error params', async () => {
    renderApp('/settings?space=alpha')
    await screen.findByLabelText('Interwał')

    changeAndBlur('Interwał', '400')

    expect(await screen.findByText('Poza planem: maksymalnie 300 s.')).toBeDefined()
  })
})

describe('settings validation', () => {
  it('the settings page shows a message for a validation error', async () => {
    renderApp('/settings?space=alpha')
    await screen.findByLabelText('Strefa czasowa')

    changeAndBlur('Strefa czasowa', 'Not/AZone')

    expect(await screen.findByText('Nieprawidłowa wartość.')).toBeDefined()
    expect(screen.getByDisplayValue('Not/AZone')).toBeDefined()

    fireEvent.blur(screen.getByLabelText('Strefa czasowa'))

    expect(screen.getByDisplayValue('Not/AZone')).toBeDefined()
    expect(screen.getByText('Nieprawidłowa wartość.')).toBeDefined()
    expect(api.writes).toEqual([{ key: 'timezone', value: 'Not/AZone' }])
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
