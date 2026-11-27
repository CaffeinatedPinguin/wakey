import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import i18n from '../app/i18n'
import type { ChecksApiMock } from './checksApiMock'
import { countsFor, createChecksApiMock, makeCheck } from './checksApiMock'
import { installMockApi } from './mockApi'
import { renderApp } from './renderApp'
import { installViewport } from './viewport'

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

let api: ChecksApiMock

function detailCheck() {
  return makeCheck({
    id: 'c1',
    lastResult: { checkedAt: '2026-11-25T11:59:48Z', state: 'DOWN', durationMs: 240 },
  })
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

describe('the check details page', () => {
  it('the check details page shows the check from the API', async () => {
    api.state.checks = [detailCheck()]
    api.state.counts = countsFor(api.state.checks)
    renderApp('/checks/c1?space=alpha')

    expect(await screen.findByRole('heading', { level: 1, name: 'API płatności' })).toBeDefined()
    expect(screen.getByText('https://payments.example/health')).toBeDefined()
    expect(screen.getAllByText('Nie działa').length).toBeGreaterThan(0)
    expect(screen.getByText('60 s')).toBeDefined()
  })

  it('a missing check shows the not found view', async () => {
    renderApp('/checks/missing?space=alpha')

    expect(await screen.findByRole('heading', { name: 'Nie znaleziono checka' })).toBeDefined()
  })

  it('the check details page without a space shows the missing space state', async () => {
    api.state.spaces = []
    renderApp('/checks/c1')

    expect(await screen.findByRole('heading', { name: 'Brak przestrzeni' })).toBeDefined()
    expect(screen.queryByRole('status', { name: 'Ładowanie checka…' })).toBeNull()
  })

  it('the details page shows only tabs that have data', async () => {
    // given a check whose detail endpoints do not exist yet
    api.state.checks = [detailCheck()]
    renderApp('/checks/c1?space=alpha')
    await screen.findByRole('heading', { level: 1, name: 'API płatności' })

    // then no empty tab is rendered
    expect(screen.queryAllByRole('tab')).toHaveLength(0)
  })
})

function givenCheck(): void {
  api.state.checks = [detailCheck()]
  api.state.counts = countsFor(api.state.checks)
}

function holdPause(gate: Promise<void>): void {
  installMockApi((request) => {
    const reply = api.handler(request)
    const isPause = request.method.toUpperCase() === 'POST' && request.url.endsWith('/pause')
    return isPause ? { ...reply, waitFor: gate } : reply
  })
}

async function findPauseButton(): Promise<HTMLElement> {
  await screen.findByRole('heading', { level: 1, name: 'API płatności' })
  return screen.getByRole('button', { name: 'Wstrzymaj' })
}

async function openDeleteDialog(): Promise<HTMLElement> {
  fireEvent.click(screen.getByRole('button', { name: 'Usuń' }))
  return screen.findByRole('dialog')
}

describe('the check details page action states', () => {
  it('the details actions are disabled while one is running', async () => {
    // given a check and a pause request held open
    givenCheck()
    const gate = deferred()
    holdPause(gate.promise)
    renderApp('/checks/c1?space=alpha')
    const pause = await findPauseButton()

    // when the action is clicked twice
    fireEvent.click(pause)
    await waitFor(() => {
      expect(pause.hasAttribute('disabled')).toBe(true)
    })
    fireEvent.click(pause)

    // then only one request is sent
    gate.resolve()
    await waitFor(() => {
      expect(api.state.pauseCalls).toEqual(['c1'])
    })
  })
})

describe('the check details page deletion', () => {
  it('deleting a check from the details page returns to the list', async () => {
    // given a check
    givenCheck()
    const view = renderApp('/checks/c1?space=alpha')
    await screen.findByRole('heading', { level: 1, name: 'API płatności' })

    // when the deletion is confirmed
    const dialog = await openDeleteDialog()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Usuń' }))

    // then the details page returns to the list of the same space
    await waitFor(() => {
      expect(api.state.deleteCalls).toEqual(['c1'])
    })
    await waitFor(() => {
      expect(view.router.state.location.pathname).toBe('/checks')
    })
    expect(await screen.findByRole('heading', { level: 1, name: 'Checki' })).toBeDefined()
    expect(view.router.state.location.search).toMatchObject({ space: 'alpha' })
  })
})
