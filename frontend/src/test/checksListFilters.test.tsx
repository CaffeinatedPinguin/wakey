import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it } from 'vitest'
import i18n from '../app/i18n'
import type { ChecksApiMock } from './checksApiMock'
import { countsFor, createChecksApiMock, makeCheck } from './checksApiMock'
import { installMockApi } from './mockApi'
import { renderApp } from './renderApp'
import { installViewport } from './viewport'

const DESKTOP_WIDTH = 1024

let api: ChecksApiMock

function installChecks(checks: ReturnType<typeof makeCheck>[]): void {
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

it('the checks list reads its filters from the typed search params', async () => {
  installChecks([makeCheck({ id: 'c1' })])
  const view = renderApp('/checks?space=alpha&q=pay&state=DOWN')
  await screen.findByText('API płatności')

  await waitFor(() => {
    const reached = api.state.listCalls.some(
      (url) => url.includes('q=pay') && url.includes('state=DOWN'),
    )
    expect(reached).toBe(true)
  })

  fireEvent.change(screen.getByLabelText('Szukaj'), { target: { value: 'panel' } })

  await waitFor(() => {
    expect(view.router.state.location.search).toMatchObject({
      space: 'alpha',
      q: 'panel',
      state: ['DOWN'],
    })
  })
  await waitFor(() => {
    expect(api.state.listCalls.some((url) => url.includes('q=panel'))).toBe(true)
  })
})

it('the check filters use the contract values in the URL', async () => {
  // given a URL that already carries the contract values
  installChecks([makeCheck({ id: 'c1' })])
  const view = renderApp('/checks?space=alpha&state=DOWN&type=HTTP&pausedReason=MANUAL')
  await screen.findByText('API płatności')

  // then the URL keeps the contract values and the API query repeats them
  expect(view.router.state.location.search).toMatchObject({
    space: 'alpha',
    state: ['DOWN'],
    type: ['HTTP'],
    pausedReason: ['MANUAL'],
  })
  await waitFor(() => {
    const reached = api.state.listCalls.some(
      (url) =>
        url.includes('state=DOWN') &&
        url.includes('type=HTTP') &&
        url.includes('pausedReason=MANUAL'),
    )
    expect(reached).toBe(true)
  })
})

it('tags and groups keep their case in the URL and the API call', async () => {
  installChecks([makeCheck({ id: 'c1' })])
  const view = renderApp('/checks?space=alpha&tag=Prod&group=Payments')
  await screen.findByText('API płatności')

  expect(view.router.state.location.search).toMatchObject({
    space: 'alpha',
    tag: ['Prod'],
    group: ['Payments'],
  })
  await waitFor(() => {
    const reached = api.state.listCalls.some(
      (url) => url.includes('tag=Prod') && url.includes('group=Payments'),
    )
    expect(reached).toBe(true)
  })
})

it('selecting a second state replaces the first one', async () => {
  installChecks([makeCheck({ id: 'c1' })])
  const view = renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')

  fireEvent.click(screen.getByRole('checkbox', { name: 'Nie działa' }))
  fireEvent.click(screen.getByRole('checkbox', { name: 'Działa' }))

  await waitFor(() => {
    expect(view.router.state.location.search).toMatchObject({ space: 'alpha', state: ['UP'] })
  })
  await waitFor(() => {
    expect(
      api.state.listCalls.some((url) => url.includes('state=UP') && !url.includes('state=DOWN')),
    ).toBe(true)
  })
})

it('removing a filter clears it from the URL and the API query', async () => {
  installChecks([makeCheck({ id: 'c1' })])
  const view = renderApp('/checks?space=alpha&state=DOWN')
  await screen.findByText('API płatności')

  fireEvent.click(screen.getByRole('checkbox', { name: 'Nie działa' }))

  await waitFor(() => {
    expect(view.router.state.location.search).not.toHaveProperty('state')
  })
  await waitFor(() => {
    expect(api.state.listCalls.some((url) => !url.includes('state='))).toBe(true)
  })
})

it('the checks page keeps the space in the URL', async () => {
  installChecks([makeCheck({ id: 'c1' })])
  const view = renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')

  fireEvent.change(screen.getByLabelText('Szukaj'), { target: { value: 'payments' } })
  await waitFor(() => {
    expect(view.router.state.location.search).toMatchObject({ space: 'alpha', q: 'payments' })
  })

  fireEvent.click(await screen.findByRole('checkbox', { name: 'Nie działa' }))
  await waitFor(() => {
    expect(view.router.state.location.search).toMatchObject({
      space: 'alpha',
      state: ['DOWN'],
    })
  })
})
