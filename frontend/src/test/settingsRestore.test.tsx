import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, expect, it } from 'vitest'
import { appConfig } from '../shared/config'
import { ErrorCode, SettingSource } from '../shared/api/generated/enums'
import type { SpaceRole } from '../shared/api/generated/enums'
import { signedInUser } from './data'
import { installMockApi } from './mockApi'
import type { MockReply, MockRequest } from './mockApi'
import { renderApp } from './renderApp'
import { createSettings } from './settingsData'
import { installViewport } from './viewport'
import i18n from '../app/i18n'
import type { Me } from '../features/auth/api/auth.schemas'
import type { SpaceSetting } from '../features/settings/api/settings.schemas'

const SPACES_URL = `${appConfig.apiBaseUrl}/spaces`
const SETTINGS_URL = `${SPACES_URL}/alpha/settings`
const DESKTOP_WIDTH = 1024

interface Deferred {
  promise: Promise<void>
  resolve: () => void
}

function deferred(): Deferred {
  let resolve: () => void = () => undefined
  const promise = new Promise<void>((done) => {
    resolve = () => {
      done()
    }
  })
  return { promise, resolve }
}

interface ApiState {
  me: Me
  role: SpaceRole | null
  settings: SpaceSetting[]
  deletes: string[]
  deleteGate: Promise<void> | null
  failDeleteOn: string | null
  resetAllRequests: number
  failResetAll: boolean
}

interface SpaceFixture {
  slug: string
  name: string
  admin: boolean
  role: SpaceRole | null
  version: number
}

function createApiState(): ApiState {
  return {
    me: { ...signedInUser },
    role: 'EDITOR',
    settings: createSettings(),
    deletes: [],
    deleteGate: null,
    failDeleteOn: null,
    resetAllRequests: 0,
    failResetAll: false,
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

function handleDelete(key: string): MockReply {
  api.deletes.push(key)
  if (api.failDeleteOn === key) {
    api.failDeleteOn = null
    return { status: 500, data: { code: 'internal_error' } }
  }
  api.settings = api.settings.map((setting) =>
    setting.key === key ? { ...setting, source: SettingSource.PLAN, value: '30' } : setting,
  )
  return api.deleteGate === null ? { status: 204 } : { status: 204, waitFor: api.deleteGate }
}

function handleResetAll(): MockReply {
  api.resetAllRequests += 1
  if (api.failResetAll) {
    return { status: 500, data: { code: ErrorCode.INTERNAL_ERROR } }
  }
  api.settings = api.settings.map((setting) =>
    setting.source === SettingSource.SPACE
      ? { ...setting, source: SettingSource.PLAN, value: '30' }
      : setting,
  )
  return { status: 204 }
}

function settingKey(url: string): string | undefined {
  const prefix = `${SETTINGS_URL}/`
  return url.startsWith(prefix) ? decodeURIComponent(url.slice(prefix.length)) : undefined
}

function handleSettingsRequest(url: string, method: string): MockReply | undefined {
  if (url === SETTINGS_URL) {
    if (method === 'delete') {
      return handleResetAll()
    }
    return { status: 200, data: { items: api.settings, nextCursor: null } }
  }
  const key = settingKey(url)
  if (key !== undefined && method === 'delete') {
    return handleDelete(key)
  }
  return undefined
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
  const settingsReply = handleSettingsRequest(url, method)
  if (settingsReply !== undefined) {
    return settingsReply
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

function restoreButton(label: string): HTMLElement {
  return within(rowFor(label)).getByRole('button', { name: 'Przywróć' })
}

async function openRestoreAll(): Promise<HTMLElement> {
  fireEvent.click(screen.getByRole('button', { name: 'Przywróć wszystkie' }))
  return screen.findByRole('dialog')
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

it('restoring a value resets it through the API', async () => {
  renderApp('/settings?space=alpha')
  await screen.findByDisplayValue('Europe/Warsaw')

  fireEvent.click(restoreButton('Strefa czasowa'))

  await waitFor(() => {
    expect(api.deletes).toEqual(['timezone'])
  })
})

it('restoring all settings sends one request', async () => {
  renderApp('/settings?space=alpha')
  await screen.findByDisplayValue('Europe/Warsaw')

  const dialog = await openRestoreAll()
  fireEvent.click(within(dialog).getByRole('button', { name: 'Tak, przywróć wszystkie' }))

  await waitFor(() => {
    expect(api.resetAllRequests).toBe(1)
  })
  expect(api.deletes).toEqual([])
})

it('a failed restore shows an error', async () => {
  renderApp('/settings?space=alpha')
  await screen.findByDisplayValue('Europe/Warsaw')
  api.failDeleteOn = 'timezone'

  fireEvent.click(restoreButton('Strefa czasowa'))

  expect(await screen.findByText('Błąd serwera. Spróbuj ponownie.')).toBeDefined()
})

it('restoring a setting cannot run twice at once', async () => {
  const gate = deferred()
  api.deleteGate = gate.promise
  renderApp('/settings?space=alpha')
  await screen.findByDisplayValue('Europe/Warsaw')

  const row = rowFor('Strefa czasowa')
  const restore = within(row).getByRole('button', { name: 'Przywróć' })
  fireEvent.click(restore)
  fireEvent.click(restore)

  await waitFor(() => {
    expect(api.deletes).toEqual(['timezone'])
  })

  gate.resolve()

  await waitFor(() => {
    expect(within(row).queryByRole('button', { name: 'Przywróć' })).toBeNull()
  })
  expect(api.deletes).toEqual(['timezone'])
})

it('a failed restore shows the server error', async () => {
  renderApp('/settings?space=alpha')
  await screen.findByDisplayValue('Europe/Warsaw')
  api.failResetAll = true

  const dialog = await openRestoreAll()
  fireEvent.click(within(dialog).getByRole('button', { name: 'Tak, przywróć wszystkie' }))

  expect(await screen.findByText('Błąd serwera. Spróbuj ponownie.')).toBeDefined()
})
