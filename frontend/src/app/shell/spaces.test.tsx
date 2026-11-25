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
  admin: boolean
  role: 'EDITOR' | 'VIEWER' | null
  version: number
}

interface ApiState {
  me: Me
  spaces: SpaceFixture[]
  pageSize: number
  calls: string[]
  putRequests: string[]
  putHeaders: string[]
  listRequests: number
  failFirstList: boolean
  createdName: string
}

const alpha: SpaceFixture = {
  slug: 'alpha',
  name: 'Alpha',
  admin: true,
  role: null,
  version: 1,
}
const beta: SpaceFixture = { slug: 'beta', name: 'Beta', admin: true, role: null, version: 1 }

function createApiState(): ApiState {
  return {
    me: { ...signedInUser },
    spaces: [],
    pageSize: 100,
    calls: [],
    putRequests: [],
    putHeaders: [],
    listRequests: 0,
    failFirstList: false,
    createdName: '',
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

function handleGet(slug: string): MockReply {
  api.calls.push(`get:${slug}`)
  const space = api.spaces.find((item) => item.slug === slug)
  if (space === undefined) {
    return { status: 404, data: { code: 'not_found' } }
  }
  return { status: 200, data: space }
}

function handlePut(slug: string, headers: Record<string, string>): MockReply {
  api.calls.push(`put:${slug}`)
  api.putRequests.push(slug)
  api.putHeaders.push(headers['if-none-match'] ?? '')
  if (headers['if-none-match'] === '*' && api.spaces.some((item) => item.slug === slug)) {
    return { status: 409, data: { code: 'slug_taken' } }
  }
  const name = api.createdName.length > 0 ? api.createdName : slug
  const space: SpaceFixture = { ...alpha, slug, name }
  if (!api.spaces.some((item) => item.slug === slug)) {
    api.spaces.push(space)
  }
  return { status: 201, data: space }
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

function handleSpace(slug: string, method: string, headers: Record<string, string>): MockReply {
  if (method === 'get') {
    return handleGet(slug)
  }
  if (method === 'put') {
    return handlePut(slug, headers)
  }
  return { status: 404, data: { code: 'not_found' } }
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
  return handleSpace(slug, request.method.toLowerCase(), request.headers)
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

describe('space creation', () => {
  it('an admin creates a space and the switcher selects it', async () => {
    api.spaces = [alpha]
    api.createdName = 'Beta'
    const { router } = renderApp('/')
    await screen.findByText('Przestrzeń: Alpha')
    openSwitcher()
    fireEvent.click(await screen.findByText('Utwórz przestrzeń'))
    fireEvent.change(await screen.findByLabelText('Nazwa'), { target: { value: 'Beta' } })
    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))
    await waitFor(() => {
      expect(router.state.location.search).toEqual({ space: 'beta' })
    })
    expect(api.putRequests).toEqual(['beta'])
    expect(api.putHeaders).toEqual(['*'])
    expect(await screen.findByText('Przestrzeń: Beta')).toBeDefined()
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'Utwórz' })).toBeNull()
    })
  })

  it('creating a space sends one PUT with If-None-Match and reports a taken slug', async () => {
    api.spaces = [alpha]
    renderApp('/')
    await screen.findByText('Przestrzeń: Alpha')
    openSwitcher()
    fireEvent.click(await screen.findByText('Utwórz przestrzeń'))
    fireEvent.change(await screen.findByLabelText('Nazwa'), { target: { value: 'Alpha' } })
    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))
    expect(await screen.findByText('Ten slug jest już zajęty.')).toBeDefined()
    expect(api.putRequests).toEqual(['alpha'])
    expect(api.putHeaders).toEqual(['*'])
    expect(api.calls.filter((call) => call.startsWith('put:'))).toEqual(['put:alpha'])
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
