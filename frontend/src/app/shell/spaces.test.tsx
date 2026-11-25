import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { Me } from '../../features/auth'
import { appConfig } from '../../shared/config'
import { signedInUser } from '../../test/data'
import { installMockApi } from '../../test/mockApi'
import type { MockReply, MockRequest } from '../../test/mockApi'
import { renderApp } from '../../test/renderApp'
import { installViewport } from '../../test/viewport'
import i18n from '../i18n'

const ME_URL = `${appConfig.apiBaseUrl}/me`
const SPACES_URL = `${appConfig.apiBaseUrl}/spaces`
const DESKTOP_WIDTH = 1024
const MOBILE_WIDTH = 375

interface SpaceFixture {
  slug: string
  name: string
  role: 'EDITOR' | 'VIEWER' | null
  version: number
}

interface ApiState {
  me: Me
  spaces: SpaceFixture[]
  pageSize: number
  listRequests: number
  failFirstList: boolean
}

const alpha: SpaceFixture = { slug: 'alpha', name: 'Alpha', role: null, version: 1 }
const beta: SpaceFixture = { slug: 'beta', name: 'Beta', role: null, version: 1 }

function createApiState(): ApiState {
  return {
    me: { ...signedInUser },
    spaces: [],
    pageSize: 100,
    listRequests: 0,
    failFirstList: false,
  }
}

let api = createApiState()

function isListUrl(url: string): boolean {
  return url === SPACES_URL || url.startsWith(`${SPACES_URL}?`)
}

function spaceSlug(url: string): string | undefined {
  const path = url.split('?')[0] ?? ''
  const prefix = `${SPACES_URL}/`
  if (!path.startsWith(prefix)) {
    return undefined
  }
  return decodeURIComponent(path.slice(prefix.length))
}

function handleList(): MockReply {
  api.listRequests += 1
  if (api.failFirstList && api.listRequests === 1) {
    return { status: 500, data: { code: 'internal_error' } }
  }
  const items = api.spaces.slice(0, api.pageSize)
  const nextCursor = api.spaces.length > api.pageSize ? 'next' : null
  return { status: 200, data: { items, nextCursor } }
}

function handleGet(slug: string): MockReply {
  const space = api.spaces.find((item) => item.slug === slug)
  if (space === undefined) {
    return { status: 404, data: { code: 'not_found' } }
  }
  return { status: 200, data: space }
}

function handleRequest(request: MockRequest): MockReply {
  if (request.url === ME_URL) {
    return { status: 200, data: api.me }
  }
  if (isListUrl(request.url)) {
    return handleList()
  }
  const slug = spaceSlug(request.url)
  if (slug === undefined) {
    return { status: 404, data: { code: 'not_found' } }
  }
  return handleGet(slug)
}

function openSwitcher(): void {
  fireEvent.click(screen.getByRole('button', { name: 'Przełącz przestrzeń' }))
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

describe('space switcher in the header', () => {
  it('the header shows the space switcher with the spaces from the API', async () => {
    api.spaces = [alpha, beta]
    renderApp('/')
    await screen.findByText('Przestrzeń: Alpha')
    openSwitcher()
    expect(await screen.findByRole('menuitem', { name: 'Alpha' })).toBeDefined()
    expect(screen.getByRole('menuitem', { name: 'Beta' })).toBeDefined()
  })

  it('choosing a space puts it in the URL and keeps the section', async () => {
    api.spaces = [alpha, beta]
    const { router } = renderApp('/checks')
    expect(await screen.findByRole('heading', { level: 1, name: 'Checki' })).toBeDefined()
    await waitFor(() => {
      expect(router.state.location.search).toEqual({ space: 'alpha' })
    })
    openSwitcher()
    fireEvent.click(await screen.findByRole('menuitem', { name: 'Beta' }))
    await waitFor(() => {
      expect(router.state.location.search).toEqual({ space: 'beta' })
    })
    expect(router.state.location.pathname).toBe('/checks')
    expect(screen.getByRole('heading', { level: 1, name: 'Checki' })).toBeDefined()
  })

  it('the switcher shows the current space name when it is not on the loaded pages', async () => {
    api.spaces = [alpha, beta]
    api.pageSize = 1
    renderApp('/checks?space=beta')

    expect(await screen.findByText('Przestrzeń: Beta')).toBeDefined()
  })
})

describe('space create permissions', () => {
  it('a non admin does not see the create action', async () => {
    api.me = { ...signedInUser, admin: false }
    api.spaces = [alpha]
    renderApp('/')
    await screen.findByText('Przestrzeń: Alpha')
    openSwitcher()
    expect(await screen.findByRole('menuitem', { name: 'Alpha' })).toBeDefined()
    expect(screen.queryByText('Utwórz przestrzeń')).toBeNull()
  })
})

describe('space resolution', () => {
  it('an unknown space in the URL shows the not found view', async () => {
    api.spaces = [alpha]
    renderApp('/checks?space=missing')

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Nie znaleziono przestrzeni' }),
    ).toBeDefined()
    expect(screen.queryByRole('heading', { level: 1, name: 'Checki' })).toBeNull()
    const link = screen.getByRole('link', { name: 'Otwórz przestrzeń Alpha' })
    expect(link.getAttribute('href')).toBe('/checks?space=alpha')
  })

  it('an unknown space is decided by the API and not by the first page of the list', async () => {
    api.spaces = [alpha, beta]
    api.pageSize = 1
    renderApp('/checks?space=beta')

    expect(await screen.findByRole('heading', { level: 1, name: 'Checki' })).toBeDefined()
    expect(
      screen.queryByRole('heading', { level: 1, name: 'Nie znaleziono przestrzeni' }),
    ).toBeNull()

    cleanup()
    api.spaces = [alpha]
    renderApp('/checks?space=missing')

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Nie znaleziono przestrzeni' }),
    ).toBeDefined()
  })
})

describe('space shell on mobile', () => {
  it('on mobile the more sheet contains the space switcher', async () => {
    installViewport(MOBILE_WIDTH)
    api.spaces = [alpha, beta]
    renderApp('/')

    fireEvent.click(await screen.findByRole('button', { name: 'Więcej' }))
    const sheet = await screen.findByRole('dialog', { name: 'Więcej' })
    const trigger = within(sheet).getByRole('button', { name: 'Przełącz przestrzeń' })
    fireEvent.click(trigger)

    const spaceSheet = await screen.findByRole('dialog', { name: 'Przestrzeń' })
    expect(await within(spaceSheet).findByRole('button', { name: 'Alpha' })).toBeDefined()
    expect(within(spaceSheet).getByRole('button', { name: 'Beta' })).toBeDefined()
  })
})

describe('space list states', () => {
  it('an empty list shows the empty state', async () => {
    renderApp('/')
    await screen.findByText('Brak przestrzeni')
    openSwitcher()

    expect(
      await screen.findByText('Nie masz jeszcze żadnej przestrzeni. Utwórz pierwszą, żeby zacząć.'),
    ).toBeDefined()
    expect(screen.getByText('Utwórz przestrzeń')).toBeDefined()
  })

  it('an API failure shows the retry state and retrying reloads the list', async () => {
    api.spaces = [alpha]
    api.failFirstList = true
    renderApp('/')
    await screen.findByText('Błąd wczytywania')
    openSwitcher()
    expect(await screen.findByText('Nie udało się wczytać listy przestrzeni.')).toBeDefined()

    fireEvent.click(await screen.findByRole('button', { name: 'Spróbuj ponownie' }))

    expect(await screen.findByRole('menuitem', { name: 'Alpha' })).toBeDefined()
    expect(api.listRequests).toBeGreaterThanOrEqual(2)
  })
})
