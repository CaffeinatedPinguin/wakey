import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it } from 'vitest'
import i18n from '../app/i18n'
import { appConfig } from '../shared/config'
import { signedInUser } from './data'
import { installMockApi } from './mockApi'
import type { MockReply, MockRequest } from './mockApi'
import { renderApp } from './renderApp'
import { installViewport } from './viewport'

const ME_URL = `${appConfig.apiBaseUrl}/me`
const SPACES_URL = `${appConfig.apiBaseUrl}/spaces`
const SETTINGS_URL = `${SPACES_URL}/alpha/settings`
const DESKTOP_WIDTH = 1024

interface Deferred {
  promise: Promise<void>
  resolve: () => void
}

interface SettingFixture {
  key: string
  planName: string
  source: string
  value: string
}

interface Write {
  key: string
  value: string
}

interface ApiState {
  settings: SettingFixture[]
  writes: Write[]
  putGate: Promise<void> | null
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

function createState(): ApiState {
  return {
    settings: [{ key: 'check.interval', planName: 'small', source: 'PLAN', value: '60' }],
    writes: [],
    putGate: null,
  }
}

let api = createState()

function space() {
  return { slug: 'alpha', name: 'Alpha', admin: signedInUser.admin, role: 'EDITOR', version: 1 }
}

function valueFrom(body: string | undefined): string {
  const parsed = JSON.parse(body ?? '{}') as { value?: unknown }
  return typeof parsed.value === 'string' ? parsed.value : ''
}

function handlePut(key: string, body: string | undefined): MockReply {
  const value = valueFrom(body)
  api.writes.push({ key, value })
  api.settings = api.settings.map((setting) =>
    setting.key === key ? { ...setting, source: 'SPACE', value } : setting,
  )
  return api.putGate === null ? { status: 204 } : { status: 204, waitFor: api.putGate }
}

function settingKey(url: string): string | undefined {
  const prefix = `${SETTINGS_URL}/`
  return url.startsWith(prefix) ? decodeURIComponent(url.slice(prefix.length)) : undefined
}

function handleRequest(request: MockRequest): MockReply {
  const url = request.url.split('?')[0] ?? ''
  if (request.url === ME_URL) {
    return { status: 200, data: signedInUser }
  }
  if (url === SPACES_URL) {
    return { status: 200, data: { items: [space()], nextCursor: null } }
  }
  if (url === `${SPACES_URL}/alpha`) {
    return { status: 200, data: space() }
  }
  if (url === SETTINGS_URL) {
    return { status: 200, data: api.settings }
  }
  if (url.startsWith(`${SETTINGS_URL}/`) && request.method.toLowerCase() === 'put') {
    return handlePut(settingKey(url) ?? '', request.body)
  }
  return { status: 404, data: { code: 'not_found' } }
}

function changeAndBlur(value: string): void {
  const input = screen.getByLabelText('Interwał')
  fireEvent.change(input, { target: { value } })
  fireEvent.blur(input)
}

beforeEach(() => {
  api = createState()
  installMockApi(handleRequest)
  installViewport(DESKTOP_WIDTH)
})

afterEach(async () => {
  cleanup()
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

it('a setting is saved on blur and not on every keystroke', async () => {
  renderApp('/settings?space=alpha')
  const interval = await screen.findByLabelText('Interwał')

  fireEvent.change(interval, { target: { value: '1' } })
  fireEvent.change(interval, { target: { value: '12' } })
  fireEvent.change(interval, { target: { value: '120' } })
  expect(api.writes).toEqual([])

  fireEvent.blur(interval)

  await waitFor(() => {
    expect(api.writes).toEqual([{ key: 'check.interval', value: '120' }])
  })
})

it('an empty setting value is not sent', async () => {
  renderApp('/settings?space=alpha')
  await screen.findByLabelText('Interwał')

  changeAndBlur('')

  expect(await screen.findByText('Wartość nie może być pusta.')).toBeDefined()
  expect(api.writes).toEqual([])
  expect(screen.getByDisplayValue('60')).toBeDefined()
})

it('a setting that did not change is not sent', async () => {
  renderApp('/settings?space=alpha')
  await screen.findByLabelText('Interwał')

  changeAndBlur('61')
  changeAndBlur('60')

  expect(api.writes).toEqual([])
})

it('saves of the same setting are applied in order', async () => {
  const gate = deferred()
  api.putGate = gate.promise
  renderApp('/settings?space=alpha')
  await screen.findByLabelText('Interwał')

  changeAndBlur('120')
  await waitFor(() => {
    expect(api.writes).toHaveLength(1)
  })

  changeAndBlur('130')
  expect(api.writes).toHaveLength(1)

  gate.resolve()

  await waitFor(() => {
    expect(api.writes).toEqual([
      { key: 'check.interval', value: '120' },
      { key: 'check.interval', value: '130' },
    ])
  })
  expect(await screen.findByDisplayValue('130')).toBeDefined()
})
