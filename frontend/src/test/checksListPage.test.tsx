import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it } from 'vitest'
import i18n from '../app/i18n'
import { contract } from '../shared/api/generated/contract'
import { BulkCheckAction } from '../shared/api/generated/enums'
import type { ChecksApiMock } from './checksApiMock'
import { countsFor, createChecksApiMock, makeCheck } from './checksApiMock'
import { installMockApi } from './mockApi'
import type { MockRequest } from './mockApi'
import { renderApp } from './renderApp'
import { installViewport } from './viewport'

const DESKTOP_WIDTH = 1024
const ALPHA_CHECKS_PATH = contract.listChecks.path.replace('{slug}', 'alpha')
const ALPHA_BULK_PATH = contract.bulkChecks.path.replace('{slug}', 'alpha')

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

let api: ChecksApiMock

function bulkIds(): string[] {
  return api.state.bulkCalls.flatMap((call) => call.ids ?? [])
}

function threeChecks() {
  return [
    makeCheck({ id: 'c1', name: 'API płatności' }),
    makeCheck({ id: 'c2', name: 'Panel klienta', state: 'UP', tags: ['publiczne'] }),
    makeCheck({ id: 'c3', name: 'Serwer SMTP', type: 'TCP', state: 'DEGRADED' }),
  ]
}

function isList(request: MockRequest): boolean {
  return (
    request.method.toUpperCase() === 'GET' &&
    (request.url.split('?')[0] ?? '') === ALPHA_CHECKS_PATH
  )
}

function installChecks(checks: ReturnType<typeof threeChecks>): void {
  api.state.checks = checks
  api.state.counts = countsFor(checks)
}

beforeEach(() => {
  api = createChecksApiMock()
  installMockApi(api.handler)
  installViewport(DESKTOP_WIDTH)
})

afterEach(async () => {
  cleanup()
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

it('the checks page lists the checks of the current space from the API', async () => {
  installChecks([makeCheck({ id: 'c1' }), makeCheck({ id: 'c2', name: 'Panel klienta' })])
  renderApp('/checks?space=alpha')

  expect(await screen.findByText('API płatności')).toBeDefined()
  expect(screen.getByText('Panel klienta')).toBeDefined()
  expect(screen.getAllByText('Nie działa').length).toBeGreaterThan(0)
  expect(api.state.listCalls.some((url) => url.startsWith(ALPHA_CHECKS_PATH))).toBe(true)
})

it('the list has no sort control until the server supports sorting', async () => {
  installChecks([makeCheck({ id: 'c1' })])
  const view = renderApp('/checks?space=alpha&sort=name')
  await screen.findByText('API płatności')

  expect(screen.queryByLabelText('Sortuj')).toBeNull()
  await waitFor(() => {
    expect(view.router.state.location.search).not.toHaveProperty('sort')
  })
  await waitFor(() => {
    expect(api.state.listCalls.some((url) => url.includes('sort='))).toBe(false)
  })
  await waitFor(() => {
    expect(api.state.countsCalls.length).toBeGreaterThan(0)
  })
  expect(api.state.countsCalls.every((url) => !url.includes('limit='))).toBe(true)
})

it('a failed bulk action shows an error and the list is refreshed once', async () => {
  installChecks([makeCheck({ id: 'c1' }), makeCheck({ id: 'c2', name: 'Panel klienta' })])
  api.state.failBulk = { status: 403, data: { code: 'forbidden' } }
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')
  const initialCalls = api.state.listCalls.length

  fireEvent.click(screen.getByRole('checkbox', { name: 'Zaznacz stronę' }))
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))

  expect(await screen.findByText('Nie masz uprawnień do wykonania tej akcji.')).toBeDefined()
  await waitFor(() => {
    expect(api.state.listCalls.length).toBe(initialCalls + 1)
  })
})

it('facet counts come from the counts endpoint', async () => {
  installChecks([makeCheck({ id: 'c1' })])
  api.state.counts = { up: 2, degraded: 1, down: 3, unknown: 0 }
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')

  expect(await screen.findByText('3')).toBeDefined()
  expect(screen.getByText('2')).toBeDefined()
  expect(screen.getByText('1')).toBeDefined()
})

it('load more fetches the next page by cursor', async () => {
  installChecks(threeChecks())
  api.state.pageSize = 2
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')

  fireEvent.click(screen.getByRole('button', { name: 'Wczytaj więcej' }))

  expect(await screen.findByText('Serwer SMTP')).toBeDefined()
  expect(api.state.listCalls.some((url) => url.includes('cursor=2'))).toBe(true)
})

it('pausing selected checks sends one bulk request and refreshes', async () => {
  installChecks([makeCheck({ id: 'c1' }), makeCheck({ id: 'c2', name: 'Panel klienta' })])
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')
  const initialCalls = api.state.listCalls.length

  fireEvent.click(screen.getByRole('checkbox', { name: 'Zaznacz stronę' }))
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))

  await waitFor(() => {
    expect(api.state.bulkCalls).toHaveLength(1)
  })
  expect([...bulkIds()].sort()).toEqual(['c1', 'c2'])
  expect(api.state.bulkCalls[0]?.action).toBe(BulkCheckAction.PAUSE)
  await waitFor(() => {
    expect(api.state.listCalls.length).toBeGreaterThan(initialCalls)
  })
})

it('selecting all results acts on every page of the result', async () => {
  installChecks(threeChecks())
  api.state.pageSize = 2
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')

  fireEvent.click(screen.getByRole('checkbox', { name: 'Zaznacz stronę' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Zaznacz wszystkie 3 wyniki' }))
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))

  await waitFor(() => {
    expect(api.state.bulkCalls).toHaveLength(1)
  })
  expect(api.state.bulkCalls[0]?.ids).toBeNull()
  expect(api.state.bulkCalls[0]?.filter).toEqual({})
})

it('the selection is cleared after a bulk action', async () => {
  installChecks([makeCheck({ id: 'c1' }), makeCheck({ id: 'c2', name: 'Panel klienta' })])
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')
  fireEvent.click(screen.getByRole('checkbox', { name: 'Zaznacz stronę' }))
  expect(await screen.findByText('Zaznaczono 2 na stronie')).toBeDefined()
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))
  await waitFor(() => {
    expect(screen.queryByText('Zaznaczono 2 na stronie')).toBeNull()
  })
  expect([...bulkIds()].sort()).toEqual(['c1', 'c2'])
})

it('a second bulk action cannot start while one is running', async () => {
  installChecks([makeCheck({ id: 'c1' }), makeCheck({ id: 'c2', name: 'Panel klienta' })])
  const gate = deferred()
  installMockApi((request) => {
    const reply = api.handler(request)
    const bulk = request.method.toUpperCase() === 'POST' && request.url.endsWith(ALPHA_BULK_PATH)
    return bulk ? { ...reply, waitFor: gate.promise } : reply
  })
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')
  fireEvent.click(screen.getByRole('checkbox', { name: 'Zaznacz stronę' }))
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))
  await waitFor(() => {
    expect(api.state.bulkCalls).toHaveLength(1)
  })
  gate.resolve()
  await waitFor(() => {
    expect([...bulkIds()].sort()).toEqual(['c1', 'c2'])
  })
})

it('the views follow the query status', async () => {
  // given a list request that is still pending
  installChecks([makeCheck({ id: 'c1' })])
  const gate = deferred()
  installMockApi((request) => {
    const reply = api.handler(request)
    return isList(request) ? { ...reply, waitFor: gate.promise } : reply
  })
  renderApp('/checks?space=alpha')

  // then the pending query shows the loading view
  expect(await screen.findByRole('status', { name: 'Ładowanie checków…' })).toBeDefined()

  // when the query succeeds the rows replace the loading view
  gate.resolve()
  expect(await screen.findByText('API płatności')).toBeDefined()
  expect(screen.queryByRole('status', { name: 'Ładowanie checków…' })).toBeNull()
})
