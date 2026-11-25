import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
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

interface SpaceFixture {
  slug: string
  name: string
  role: 'EDITOR' | 'VIEWER' | null
  version: number
}

interface ApiState {
  me: Me
  spaces: SpaceFixture[]
  createRequests: string[]
  createMethods: string[]
  createError: MockReply | null
}

const alpha: SpaceFixture = { slug: 'alpha', name: 'Alpha', role: null, version: 1 }

function createApiState(): ApiState {
  return {
    me: { ...signedInUser },
    spaces: [],
    createRequests: [],
    createMethods: [],
    createError: null,
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
  return { status: 200, data: { items: api.spaces, nextCursor: null } }
}

function handleGet(slug: string): MockReply {
  const space = api.spaces.find((item) => item.slug === slug)
  if (space === undefined) {
    return { status: 404, data: { code: 'not_found' } }
  }
  return { status: 200, data: space }
}

interface CreateBody {
  slug: string
  name: string
}

function handleCreate(method: string, body: string): MockReply {
  const payload = JSON.parse(body) as CreateBody
  api.createRequests.push(payload.slug)
  api.createMethods.push(method)
  if (api.createError !== null) {
    return api.createError
  }
  if (api.spaces.some((item) => item.slug === payload.slug)) {
    return { status: 409, data: { code: 'slug_taken' } }
  }
  const space: SpaceFixture = { slug: payload.slug, name: payload.name, role: null, version: 1 }
  api.spaces.push(space)
  return { status: 201, data: space }
}

function handleRequest(request: MockRequest): MockReply {
  if (request.url === ME_URL) {
    return { status: 200, data: api.me }
  }
  if (request.method.toLowerCase() === 'post' && isListUrl(request.url)) {
    return handleCreate(request.method, request.body)
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

async function openCreateDialog(name: string): Promise<void> {
  openSwitcher()
  fireEvent.click(await screen.findByText('Utwórz przestrzeń'))
  fireEvent.change(await screen.findByLabelText('Nazwa'), { target: { value: name } })
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

describe('space creation', () => {
  it('an admin creates a space and the switcher selects it', async () => {
    api.spaces = [alpha]
    const { router } = renderApp('/')
    await screen.findByText('Przestrzeń: Alpha')
    openSwitcher()
    fireEvent.click(await screen.findByText('Utwórz przestrzeń'))
    fireEvent.change(await screen.findByLabelText('Nazwa'), { target: { value: 'Beta' } })
    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))
    await waitFor(() => {
      expect(router.state.location.search).toEqual({ space: 'beta' })
    })
    expect(api.createRequests).toEqual(['beta'])
    expect(api.createMethods.map((method) => method.toLowerCase())).toEqual(['post'])
    expect(await screen.findByText('Przestrzeń: Beta')).toBeDefined()
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'Utwórz' })).toBeNull()
    })
  })
})

describe('space creation preconditions', () => {
  it('creating a space sends one POST and reports a taken slug', async () => {
    api.spaces = [alpha]
    renderApp('/')
    await screen.findByText('Przestrzeń: Alpha')
    openSwitcher()
    fireEvent.click(await screen.findByText('Utwórz przestrzeń'))
    fireEvent.change(await screen.findByLabelText('Nazwa'), { target: { value: 'Alpha' } })
    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))
    expect(await screen.findByText('Ten slug jest już zajęty.')).toBeDefined()
    expect(api.createRequests).toEqual(['alpha'])
    expect(api.createMethods.map((method) => method.toLowerCase())).toEqual(['post'])
  })
})

describe('space creation from a missing slug', () => {
  it('creating a space shown as missing replaces the not found view without a reload', async () => {
    api.spaces = [alpha]
    renderApp('/checks?space=acme')

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Nie znaleziono przestrzeni' }),
    ).toBeDefined()
    openSwitcher()
    fireEvent.click(await screen.findByText('Utwórz przestrzeń'))
    fireEvent.change(await screen.findByLabelText('Nazwa'), { target: { value: 'Acme' } })
    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Checki' })).toBeDefined()
    expect(
      screen.queryByRole('heading', { level: 1, name: 'Nie znaleziono przestrzeni' }),
    ).toBeNull()
  })
})

describe('create space dialog errors', () => {
  it('closing the create dialog clears the previous error', async () => {
    api.spaces = [alpha]
    renderApp('/')
    await screen.findByText('Przestrzeń: Alpha')
    openSwitcher()
    fireEvent.click(await screen.findByText('Utwórz przestrzeń'))
    fireEvent.change(await screen.findByLabelText('Nazwa'), { target: { value: 'Alpha' } })
    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))
    expect(await screen.findByText('Ten slug jest już zajęty.')).toBeDefined()

    fireEvent.click(screen.getByRole('button', { name: 'Anuluj' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).toBeNull()
    })
    openSwitcher()
    fireEvent.click(await screen.findByText('Utwórz przestrzeń'))

    expect(screen.queryByText('Ten slug jest już zajęty.')).toBeNull()
  })
})

describe('create space dialog reopening', () => {
  it('reopening the create dialog starts without the previous error', async () => {
    api.spaces = [alpha]
    api.createError = { status: 500, data: { code: 'internal_error' } }
    renderApp('/')
    await screen.findByText('Przestrzeń: Alpha')
    await openCreateDialog('Beta')
    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))
    expect(
      await screen.findByText('Nie udało się utworzyć przestrzeni. Spróbuj ponownie.'),
    ).toBeDefined()

    fireEvent.click(screen.getByRole('button', { name: 'Zamknij' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: 'Nowa przestrzeń' })).toBeNull()
    })
    await openCreateDialog('Beta')

    expect(screen.queryByText('Nie udało się utworzyć przestrzeni. Spróbuj ponownie.')).toBeNull()
  })
})

describe('create space dialog server validation', () => {
  it('server validation errors are shown on the form fields', async () => {
    api.spaces = [alpha]
    api.createError = {
      status: 400,
      data: {
        code: 'validation_failed',
        errors: [
          { code: 'invalid_name', pointer: '/name' },
          { code: 'invalid_slug', pointer: '/slug' },
        ],
      },
    }
    renderApp('/')
    await screen.findByText('Przestrzeń: Alpha')
    openSwitcher()
    fireEvent.click(await screen.findByText('Utwórz przestrzeń'))
    fireEvent.change(await screen.findByLabelText('Nazwa'), { target: { value: 'Inna nazwa' } })
    fireEvent.change(screen.getByLabelText('Slug'), { target: { value: 'inna-nazwa' } })
    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(await screen.findByText('Nazwa jest nieprawidłowa.')).toBeDefined()
    expect(
      screen.getByText(
        'Slug musi mieć 3–40 znaków: małe litery, cyfry i myślniki, zaczynać się literą i nie kończyć myślnikiem.',
      ),
    ).toBeDefined()
  })
})
