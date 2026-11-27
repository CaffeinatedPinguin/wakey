import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it } from 'vitest'
import i18n from '../app/i18n'
import { appConfig } from '../shared/config'
import { ErrorCode, SettingGroup, SettingSource, SettingType } from '../shared/api/generated/enums'
import type { SpaceSetting } from '../features/settings/api/settings.schemas'
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

interface Write {
  key: string
  value: string
}

interface ApiState {
  settings: SpaceSetting[]
  writes: Write[]
  putGate: Promise<void> | null
  failPut: boolean
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
    settings: [
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
    ],
    writes: [],
    putGate: null,
    failPut: false,
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
  if (api.failPut) {
    api.failPut = false
    return {
      status: 500,
      data: { code: ErrorCode.INTERNAL_ERROR, params: {}, errors: [] },
    }
  }
  if (key === 'timezone' && value === 'Not/AZone') {
    return {
      status: 400,
      data: { code: ErrorCode.VALIDATION_FAILED, params: {}, errors: [] },
    }
  }
  api.settings = api.settings.map((setting) =>
    setting.key === key ? { ...setting, source: SettingSource.SPACE, value } : setting,
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
    return { status: 200, data: { items: api.settings, nextCursor: null } }
  }
  if (url.startsWith(`${SETTINGS_URL}/`) && request.method.toLowerCase() === 'put') {
    return handlePut(settingKey(url) ?? '', request.body)
  }
  return { status: 404, data: { code: ErrorCode.NOT_FOUND } }
}

function changeAndBlur(label: string, value: string): void {
  const input = screen.getByLabelText(label)
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

it('typing in a setting field sends nothing until the value is committed', async () => {
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

it('pressing Enter commits the value once', async () => {
  renderApp('/settings?space=alpha')
  const interval = await screen.findByLabelText('Interwał')

  fireEvent.change(interval, { target: { value: '120' } })
  fireEvent.keyDown(interval, { key: 'Enter' })

  await waitFor(() => {
    expect(api.writes).toEqual([{ key: 'check.interval', value: '120' }])
  })

  fireEvent.keyDown(interval, { key: 'Enter' })
  fireEvent.blur(interval)

  expect(api.writes).toHaveLength(1)
})

it('a rejected value stays in the field with its error', async () => {
  renderApp('/settings?space=alpha')
  const timezone = await screen.findByLabelText('Strefa czasowa')

  fireEvent.change(timezone, { target: { value: 'Not/AZone' } })
  fireEvent.blur(timezone)

  expect(await screen.findByText('Nieprawidłowa wartość.')).toBeDefined()
  expect(screen.getByDisplayValue('Not/AZone')).toBeDefined()

  fireEvent.blur(timezone)

  expect(screen.getByDisplayValue('Not/AZone')).toBeDefined()
  expect(api.writes).toEqual([{ key: 'timezone', value: 'Not/AZone' }])
})

it('a failed setting save shows an error and the same value can be sent again', async () => {
  // given a server that fails the first save and accepts the next one
  renderApp('/settings?space=alpha')
  const timezone = await screen.findByLabelText('Strefa czasowa')
  api.failPut = true

  // when the value is committed twice without editing it in between
  fireEvent.change(timezone, { target: { value: 'Europe/London' } })
  fireEvent.blur(timezone)

  // then the field error is shown and the value stays editable
  expect(await screen.findByText('Nie udało się zapisać wartości. Spróbuj ponownie.')).toBeDefined()
  expect(screen.getByDisplayValue('Europe/London')).toBeDefined()

  fireEvent.blur(timezone)

  // then the same value is sent again
  await waitFor(() => {
    expect(api.writes).toEqual([
      { key: 'timezone', value: 'Europe/London' },
      { key: 'timezone', value: 'Europe/London' },
    ])
  })
})

it('an empty setting value is not sent', async () => {
  renderApp('/settings?space=alpha')
  await screen.findByLabelText('Interwał')

  changeAndBlur('Interwał', '')

  expect(await screen.findByText('Wartość nie może być pusta.')).toBeDefined()
  expect(api.writes).toEqual([])
  expect(screen.getByDisplayValue('60')).toBeDefined()
})

it('a setting that did not change is not sent', async () => {
  renderApp('/settings?space=alpha')
  await screen.findByLabelText('Interwał')

  changeAndBlur('Interwał', '61')
  changeAndBlur('Interwał', '60')

  expect(api.writes).toEqual([])
})

it('two commits of one field never overlap', async () => {
  const gate = deferred()
  api.putGate = gate.promise
  renderApp('/settings?space=alpha')
  await screen.findByLabelText('Interwał')

  changeAndBlur('Interwał', '120')
  await waitFor(() => {
    expect(api.writes).toHaveLength(1)
  })

  changeAndBlur('Interwał', '130')
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
