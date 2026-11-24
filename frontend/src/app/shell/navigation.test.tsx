import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { appConfig } from '../../shared/config'
import { signedInUser, unauthorizedBody } from '../../test/data'
import { installMockApi } from '../../test/mockApi'
import type { MockReply, MockRequest } from '../../test/mockApi'
import { renderApp } from '../../test/renderApp'
import { installViewport } from '../../test/viewport'
import i18n from '../i18n'

const ME_URL = `${appConfig.apiBaseUrl}/me`
const DESKTOP_WIDTH = 1024
const MOBILE_WIDTH = 375

let meStatus = 200

function handleRequest(request: MockRequest): MockReply {
  if (request.url === ME_URL) {
    return meStatus === 200
      ? { status: 200, data: signedInUser }
      : { status: meStatus, data: unauthorizedBody }
  }
  return { status: 404, data: { code: 'not_found' } }
}

function linkNames(container: HTMLElement): string[] {
  return within(container)
    .getAllByRole('link')
    .map((link) => link.textContent)
}

async function openMobileMoreSheet(): Promise<HTMLElement> {
  installViewport(MOBILE_WIDTH)
  renderApp('/')
  fireEvent.click(await screen.findByRole('button', { name: 'Więcej' }))
  return screen.findByRole('dialog', { name: 'Więcej' })
}

beforeEach(() => {
  meStatus = 200
  installMockApi(handleRequest)
  installViewport(DESKTOP_WIDTH)
})

afterEach(async () => {
  cleanup()
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

describe('panel navigation on desktop', () => {
  it('the panel navigation lists the five sections on desktop', async () => {
    renderApp('/')

    const nav = await screen.findByRole('navigation', { name: 'Nawigacja główna' })

    expect(linkNames(nav)).toEqual([
      'Przegląd',
      'Checki',
      'Incydenty',
      'Status pages',
      'Ustawienia',
    ])
  })
})

describe('panel navigation on mobile', () => {
  it('on mobile the bottom bar has overview, checks, incidents and more', async () => {
    installViewport(MOBILE_WIDTH)
    renderApp('/')

    const nav = await screen.findByRole('navigation', { name: 'Nawigacja główna' })

    expect(linkNames(nav)).toEqual(['Przegląd', 'Checki', 'Incydenty'])
    expect(within(nav).getByRole('button', { name: 'Więcej' })).toBeDefined()
    expect(within(nav).queryByRole('link', { name: 'Status pages' })).toBeNull()
    expect(within(nav).queryByRole('link', { name: 'Ustawienia' })).toBeNull()
  })

  it('the more sheet lists status pages and settings and closes after a choice', async () => {
    const sheet = await openMobileMoreSheet()

    expect(within(sheet).getByRole('link', { name: 'Status pages' })).toBeDefined()
    fireEvent.click(within(sheet).getByRole('link', { name: 'Ustawienia' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Ustawienia' })).toBeDefined()
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: 'Więcej' })).toBeNull()
    })
  })
})

describe('panel sections', () => {
  it('an empty section shows its title and a way back to the overview', async () => {
    const sections = [
      { path: '/checks', title: 'Checki' },
      { path: '/incidents', title: 'Incydenty' },
      { path: '/status-pages', title: 'Status pages' },
      { path: '/settings', title: 'Ustawienia' },
    ]

    for (const section of sections) {
      const view = renderApp(section.path)
      expect(await screen.findByRole('heading', { level: 1, name: section.title })).toBeDefined()
      const backLink = screen.getByRole('link', { name: 'Wróć do Przeglądu' })
      expect(backLink.getAttribute('href')).toBe('/')
      view.unmount()
    }
  })

  it('a section path keeps the sign-in protection', async () => {
    meStatus = 401
    renderApp('/checks')

    expect(await screen.findByRole('link', { name: 'Zaloguj się' })).toBeDefined()
    expect(screen.queryByRole('heading', { level: 1, name: 'Checki' })).toBeNull()
  })
})

describe('panel navigation state', () => {
  it('the active section is marked with aria-current', async () => {
    renderApp('/checks')

    const nav = await screen.findByRole('navigation', { name: 'Nawigacja główna' })
    const checksLink = within(nav).getByRole('link', { name: 'Checki' })
    const overviewLink = within(nav).getByRole('link', { name: 'Przegląd' })

    expect(checksLink.getAttribute('aria-current')).toBe('page')
    expect(overviewLink.getAttribute('aria-current')).toBeNull()
  })
})
