import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { signedInUser } from '../../test/data'
import { installMockApi } from '../../test/mockApi'
import type { MockReply, MockRequest } from '../../test/mockApi'
import { renderApp } from '../../test/renderApp'
import { installViewport } from '../../test/viewport'
import { appConfig } from '../../shared/config'
import { StatusBadge } from '../../shared/ui/StatusBadge'
import type { StatusState } from '../../shared/ui/StatusBadge'
import { AppThemeProvider } from '../App'
import i18n from '../i18n'

const ME_URL = `${appConfig.apiBaseUrl}/me`
const DESKTOP_WIDTH = 1024

let logoutRequestCount = 0

function handleRequest(request: MockRequest): MockReply {
  const method = request.method.toLowerCase()
  if (request.url === ME_URL) {
    return { status: 200, data: signedInUser }
  }
  if (request.url === appConfig.logoutUrl && method === 'post') {
    logoutRequestCount += 1
    return { status: 204 }
  }
  return { status: 404, data: { code: 'not_found' } }
}

beforeEach(() => {
  logoutRequestCount = 0
  installMockApi(handleRequest)
  installViewport(DESKTOP_WIDTH)
})

afterEach(async () => {
  cleanup()
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

describe('panel shell', () => {
  it('the panel shell shows navigation, language, theme and profile on desktop', async () => {
    renderApp('/')

    expect(await screen.findByRole('link', { name: 'Przegląd' })).toBeDefined()
    expect(screen.getByRole('radio', { name: 'PL' })).toBeDefined()
    expect(screen.getByRole('radio', { name: 'Jasny' })).toBeDefined()
    expect(screen.getByRole('button', { name: 'Profil' })).toBeDefined()
  })

  it('on a phone the panel shows the bottom bar and the more sheet', async () => {
    await openPhoneMoreSheet()

    const sheet = await screen.findByRole('dialog', { name: 'Więcej' })
    expect(within(sheet).getByText('Język')).toBeDefined()
    expect(within(sheet).getByText('Motyw')).toBeDefined()
    expect(within(sheet).getByText('Wyloguj')).toBeDefined()
  })

  it('the more sheet opens from the bottom and does not cover the whole screen', async () => {
    await openPhoneMoreSheet()

    const sheet = await screen.findByRole('dialog', { name: 'Więcej' })
    expect(sheet.className).toContain('mantine-Drawer-content')
    expect(sheet.style.flex).toBe('0 0 auto')
    expect(sheet.style.height).toBe('auto')
    expect(sheet.style.height).not.toBe('100%')
    expect(sheet.style.height).not.toBe('100dvh')
    expect(sheet.style.maxHeight).toBe('80dvh')
  })
})

async function openPhoneMoreSheet(): Promise<void> {
  installViewport(375)
  renderApp('/')
  expect(await screen.findByRole('button', { name: 'Więcej' })).toBeDefined()
  expect(screen.queryByRole('radio', { name: 'PL' })).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Więcej' }))
}

describe('panel shell breakpoints', () => {
  it('at exactly 768 px the panel shows the desktop navigation', async () => {
    installViewport(768)
    renderApp('/')

    expect(await screen.findByRole('link', { name: 'Przegląd' })).toBeDefined()
    expect(screen.getByRole('radio', { name: 'PL' })).toBeDefined()
    expect(screen.queryByRole('button', { name: 'Więcej' })).toBeNull()
  })

  it('just below 768 px the panel uses the bottom bar', async () => {
    installViewport(767)
    renderApp('/')

    expect(await screen.findByRole('link', { name: 'Przegląd' })).toBeDefined()
    expect(await screen.findByRole('button', { name: 'Więcej' })).toBeDefined()
    expect(screen.queryByRole('radio', { name: 'PL' })).toBeNull()
  })

  it('at a fractional width of 767.5 px the panel still uses the phone layout', async () => {
    installViewport(767.5)
    renderApp('/')

    expect(await screen.findByRole('button', { name: 'Więcej' })).toBeDefined()
    expect(screen.getByRole('link', { name: 'Przegląd' })).toBeDefined()
    expect(screen.queryByRole('radio', { name: 'PL' })).toBeNull()
  })
})

describe('panel shell updates', () => {
  it('switching the language changes the shell texts', async () => {
    renderApp('/')

    fireEvent.click(await screen.findByRole('radio', { name: 'EN' }))

    expect(await screen.findByRole('link', { name: 'Overview' })).toBeDefined()
  })

  it('the theme toggle switches between light and dark', async () => {
    renderApp('/')

    fireEvent.click(await screen.findByRole('radio', { name: 'Ciemny' }))
    await waitFor(() => {
      expect(document.documentElement.getAttribute('data-mantine-color-scheme')).toBe('dark')
    })

    fireEvent.click(screen.getByRole('radio', { name: 'Jasny' }))
    await waitFor(() => {
      expect(document.documentElement.getAttribute('data-mantine-color-scheme')).toBe('light')
    })
  })

  it('sign out from the profile menu shows the logged-out screen', async () => {
    renderApp('/')

    fireEvent.click(await screen.findByRole('button', { name: 'Profil' }))
    fireEvent.click(await screen.findByText('Wyloguj'))

    expect(await screen.findByRole('heading', { name: 'Wylogowano' })).toBeDefined()
  })
})

describe('document language', () => {
  it('the document language follows the app language', async () => {
    renderApp('/')

    expect(document.documentElement.lang).toBe('pl')
    fireEvent.click(await screen.findByRole('radio', { name: 'EN' }))
    await waitFor(() => {
      expect(document.documentElement.lang).toBe('en')
    })
  })
})

describe('signing out once', () => {
  it('sign out sends one request even after a double click', async () => {
    installViewport(375)
    renderApp('/')

    fireEvent.click(await screen.findByRole('button', { name: 'Więcej' }))
    const sheet = await screen.findByRole('dialog', { name: 'Więcej' })
    const logoutButton = within(sheet).getByRole('button', { name: 'Wyloguj' })
    fireEvent.click(logoutButton)
    fireEvent.click(logoutButton)

    expect(await screen.findByRole('heading', { name: 'Wylogowano' })).toBeDefined()
    expect(logoutRequestCount).toBe(1)
  })
})

describe('public shell', () => {
  it('the login screen uses the public shell without panel navigation', async () => {
    renderApp('/login')

    expect(await screen.findByRole('heading', { level: 1, name: 'wakey' })).toBeDefined()
    expect(screen.queryByRole('navigation')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Profil' })).toBeNull()
    expect(screen.getByRole('radio', { name: 'PL' })).toBeDefined()
  })
})

describe('status badge', () => {
  it('a state badge shows an icon and a text for every state', () => {
    const states: StatusState[] = ['up', 'degraded', 'down', 'unknown', 'maintenance']
    const labels = ['Działa', 'Działa z problemami', 'Nie działa', 'Stan nieznany', 'Prace planowe']
    const { container } = render(
      <AppThemeProvider>
        {states.map((state) => (
          <StatusBadge key={state} state={state} />
        ))}
      </AppThemeProvider>,
    )

    for (const label of labels) {
      expect(screen.getByText(label)).toBeDefined()
    }
    expect(container.querySelectorAll('svg').length).toBeGreaterThanOrEqual(states.length)
  })
})
