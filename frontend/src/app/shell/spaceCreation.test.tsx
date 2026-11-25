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
  putRequests: string[]
  putHeaders: string[]
  putError: MockReply | null
  createdName: string
}

const alpha: SpaceFixture = { slug: 'alpha', name: 'Alpha', role: null, version: 1 }

function createApiState(): ApiState {
  return {
    me: { ...signedInUser },
    spaces: [],
    putRequests: [],
    putHeaders: [],
    putError: null,
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

function handlePut(slug: string, headers: Record<string, string>): MockReply {
  api.putRequests.push(slug)
  api.putHeaders.push(headers['if-none-match'] ?? '')
  if (api.putError !== null) {
    return api.putError
  }
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
  if (request.method.toLowerCase() === 'put') {
    return handlePut(slug, request.headers)
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
})

describe('space creation preconditions', () => {
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
  })
})

describe('space creation from a missing slug', () => {
  it('creating a space shown as missing replaces the not found view without a reload', async () => {
    api.spaces = [alpha]
    api.createdName = 'Acme'
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
    api.putError = { status: 500, data: { code: 'internal_error' } }
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
    api.putError = {
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
