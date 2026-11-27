import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, expect, it } from 'vitest'
import i18n from '../app/i18n'
import { contract } from '../shared/api/generated/contract'
import { BulkCheckAction, ErrorCode } from '../shared/api/generated/enums'
import type { ChecksApiMock } from './checksApiMock'
import { countsFor, createChecksApiMock, makeCheck } from './checksApiMock'
import { installMockApi } from './mockApi'
import type { MockRequest } from './mockApi'
import { renderApp } from './renderApp'
import { installViewport } from './viewport'

const DESKTOP_WIDTH = 1024
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

function isBulk(request: MockRequest): boolean {
  return request.method.toUpperCase() === 'POST' && request.url.endsWith(ALPHA_BULK_PATH)
}

let api: ChecksApiMock

function threeChecks(): ReturnType<typeof makeCheck>[] {
  return [
    makeCheck({ id: 'c1', name: 'API płatności' }),
    makeCheck({ id: 'c2', name: 'Panel klienta', state: 'UP', tags: ['publiczne'] }),
    makeCheck({ id: 'c3', name: 'Serwer SMTP', type: 'TCP', state: 'DEGRADED' }),
  ]
}

function installChecks(checks: ReturnType<typeof makeCheck>[]): void {
  api.state.checks = checks
  api.state.counts = countsFor(checks)
}

function selectPage(): void {
  fireEvent.click(screen.getByRole('checkbox', { name: 'Zaznacz stronę' }))
}

async function selectAll(total: number): Promise<void> {
  const label = i18n.t('checks.list.selection.selectAll', { count: total, total })
  fireEvent.click(await screen.findByRole('button', { name: label }))
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

it('a bulk action on the selected page sends the ids once', async () => {
  // given a page of checks
  installChecks(threeChecks())
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')

  // when the selected page is paused
  selectPage()
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))

  // then one request carries the selected ids and no filter
  await waitFor(() => {
    expect(api.state.bulkCalls).toHaveLength(1)
  })
  const call = api.state.bulkCalls[0]
  expect(call?.action).toBe(BulkCheckAction.PAUSE)
  expect(call?.ids).toEqual(['c1', 'c2', 'c3'])
  expect(call?.filter).toBeNull()
})

it('a bulk action on all results sends the current filter', async () => {
  // given a filtered list
  installChecks(threeChecks())
  renderApp('/checks?space=alpha&state=DOWN&type=HTTP')
  await screen.findByText('API płatności')

  // when all results are paused
  selectPage()
  await selectAll(3)
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))

  // then one request carries the filter instead of ids
  await waitFor(() => {
    expect(api.state.bulkCalls).toHaveLength(1)
  })
  const call = api.state.bulkCalls[0]
  expect(call?.ids).toBeNull()
  expect(call?.filter).toEqual({ state: 'DOWN', type: 'HTTP' })
})

it('a bulk action on all results sends the confirmed count', async () => {
  // given a counter larger than one page
  installChecks(threeChecks())
  api.state.counts = { up: 0, degraded: 0, down: 7, unknown: 0 }
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')
  selectPage()
  await selectAll(7)

  // when all results are paused
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))

  // then the request carries the count the user confirmed in the bar
  await waitFor(() => {
    expect(api.state.bulkCalls).toHaveLength(1)
  })
  const call = api.state.bulkCalls[0]
  expect(call?.ids).toBeNull()
  expect(call?.expectedCount).toBe(7)
})

it('a changed count stops the bulk action and keeps the selection', async () => {
  // given all results selected but the server no longer matches the count
  installChecks(threeChecks())
  api.state.counts = { up: 0, degraded: 0, down: 7, unknown: 0 }
  api.state.failBulk = {
    status: 409,
    data: { code: ErrorCode.BULK_COUNT_CHANGED, params: { expected: 7, actual: 2 } },
  }
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')
  selectPage()
  await selectAll(7)
  const listCallsBefore = api.state.listCalls.length
  const countsCallsBefore = api.state.countsCalls.length

  // when the bulk action runs
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))

  // then the changed count is reported, the list is refreshed and the selection stays
  expect(
    await screen.findByText(
      'Lista wyników zmieniła się: oczekiwano 7, jest 2. Zaznaczenie zostało.',
    ),
  ).toBeDefined()
  expect(api.state.bulkCalls).toHaveLength(1)
  await waitFor(() => {
    expect(api.state.listCalls.length).toBeGreaterThan(listCallsBefore)
  })
  await waitFor(() => {
    expect(api.state.countsCalls.length).toBeGreaterThan(countsCallsBefore)
  })
  expect(await screen.findByText('Zaznaczono wszystkie 7 wyników')).toBeDefined()
})

it('a bulk action reports the affected count', async () => {
  // given a page of three checks
  installChecks(threeChecks())
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')

  // when the page is paused
  selectPage()
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))

  // then the result reports how many checks the action changed
  expect(await screen.findByText('Zmieniono 3 checki')).toBeDefined()
})

it('a failed bulk action keeps the selection', async () => {
  // given a bulk request the server rejects
  installChecks(threeChecks())
  api.state.failBulk = { status: 403, data: { code: ErrorCode.FORBIDDEN } }
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')
  selectPage()
  expect(await screen.findByText('Zaznaczono 3 na stronie')).toBeDefined()

  // when the action fails
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))

  // then the error is shown and the selection stays for a retry
  expect(await screen.findByText('Nie masz uprawnień do wykonania tej akcji.')).toBeDefined()
  expect(screen.getByText('Zaznaczono 3 na stronie')).toBeDefined()
})

it('a row action cannot start while a bulk action runs', async () => {
  // given a bulk request held open
  installChecks([makeCheck({ id: 'c1' }), makeCheck({ id: 'c2', name: 'Panel klienta' })])
  const gate = deferred()
  installMockApi((request) => {
    const reply = api.handler(request)
    return isBulk(request) ? { ...reply, waitFor: gate.promise } : reply
  })
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')
  selectPage()
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))
  await waitFor(() => {
    expect(api.state.bulkCalls).toHaveLength(1)
  })

  // when a row action is clicked during the bulk run
  fireEvent.click(screen.getByRole('button', { name: 'Akcje checka API płatności' }))
  fireEvent.click(await screen.findByRole('menuitem', { name: 'Pauza' }))

  // then the row action is not sent
  gate.resolve()
  await waitFor(() => {
    expect(api.state.bulkCalls).toHaveLength(1)
  })
  expect(api.state.pauseCalls).toEqual([])
})

it('the delete dialog counts all results from the list counters', async () => {
  // given a counter larger than one page
  installChecks(threeChecks())
  api.state.counts = { up: 0, degraded: 0, down: 7, unknown: 0 }
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')

  // when all results are selected for deletion
  selectPage()
  await selectAll(7)
  fireEvent.click(screen.getByRole('button', { name: 'Usuń' }))
  const dialog = await screen.findByRole('dialog')

  // then the dialog and the request use the counter, not the loaded page
  expect(within(dialog).getByText('Usunąć 7 checków? Tej operacji nie można cofnąć.')).toBeDefined()
  fireEvent.click(within(dialog).getByRole('button', { name: 'Usuń' }))
  await waitFor(() => {
    expect(api.state.bulkCalls).toHaveLength(1)
  })
  expect(api.state.bulkCalls[0]?.ids).toBeNull()
})
