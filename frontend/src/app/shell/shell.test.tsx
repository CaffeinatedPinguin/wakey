import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { signedInUser } from '../../test/data'
import { installMockApi } from '../../test/mockApi'
import type { MockReply, MockRequest } from '../../test/mockApi'
import { renderApp } from '../../test/renderApp'
import { installViewport } from '../../test/viewport'
import { ErrorCode } from '../../shared/api/generated/enums'
import { appConfig } from '../../shared/config'
import { StatusBadge } from '../../shared/ui/StatusBadge'
import { StatusState } from '../../shared/ui'
import { AppThemeProvider } from '../App'
import i18n from '../i18n'

const ME_URL = `${appConfig.apiBaseUrl}/me`
const DESKTOP_WIDTH = 1024
const MOBILE_WIDTH = 375

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
  return { status: 404, data: { code: ErrorCode.NOT_FOUND } }
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

async function openMobileMoreSheet(): Promise<HTMLElement> {
  installViewport(MOBILE_WIDTH)
  renderApp('/')
  expect(await screen.findByRole('button', { name: 'Więcej' })).toBeDefined()
  expect(screen.queryByRole('radio', { name: 'PL' })).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Więcej' }))
  return screen.findByRole('dialog', { name: 'Więcej' })
}

describe('panel shell', () => {
  it('the panel shell shows navigation, language, theme and profile on desktop', async () => {
    renderApp('/')

    expect(await screen.findByRole('link', { name: 'Przegląd' })).toBeDefined()
    expect(screen.getByRole('radio', { name: 'PL' })).toBeDefined()
    expect(screen.getByRole('radio', { name: 'Jasny' })).toBeDefined()
    expect(screen.getByRole('button', { name: 'Profil' })).toBeDefined()
  })

  it('on mobile the panel shows the bottom bar and the more sheet', async () => {
    const sheet = await openMobileMoreSheet()

    expect(within(sheet).getByText('Język')).toBeDefined()
    expect(within(sheet).getByText('Motyw')).toBeDefined()
    expect(within(sheet).getByText('Wyloguj')).toBeDefined()
  })
})

describe('panel shell mobile layout', () => {
  it('the mobile bottom bar is a labelled navigation landmark', async () => {
    installViewport(MOBILE_WIDTH)
    const mobile = renderApp('/')
    expect(await screen.findByRole('navigation', { name: 'Nawigacja główna' })).toBeDefined()
    mobile.unmount()

    installViewport(DESKTOP_WIDTH)
    renderApp('/')
    expect(await screen.findByRole('navigation', { name: 'Nawigacja główna' })).toBeDefined()
  })

  it('the more sheet closes when the layout leaves mobile', async () => {
    await openMobileMoreSheet()

    act(() => {
      installViewport(DESKTOP_WIDTH)
    })
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'Więcej' })).toBeNull()
    })

    act(() => {
      installViewport(MOBILE_WIDTH)
    })
    expect(await screen.findByRole('button', { name: 'Więcej' })).toBeDefined()
    expect(screen.queryByRole('dialog', { name: 'Więcej' })).toBeNull()
  })
})

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

  it('at a fractional width of 767.5 px the panel still uses the mobile layout', async () => {
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
    const sheet = await openMobileMoreSheet()

    const logoutButton = within(sheet).getByRole('button', { name: 'Wyloguj' })
    fireEvent.click(logoutButton)
    fireEvent.click(logoutButton)

    expect(await screen.findByRole('heading', { name: 'Wylogowano' })).toBeDefined()
    expect(logoutRequestCount).toBe(1)
  })

  it('sign out sends one request when clicked in the profile menu and the sheet', async () => {
    const sheet = await openMobileMoreSheet()
    const sheetLogout = within(sheet).getByRole('button', { name: 'Wyloguj' })

    fireEvent.click(screen.getByRole('button', { name: 'Profil' }))
    const menuLogout = await screen.findByRole('menuitem', { name: 'Wyloguj' })
    fireEvent.click(sheetLogout)
    fireEvent.click(menuLogout)

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
    const states = Object.values(StatusState)
    const labels: Record<StatusState, string> = {
      [StatusState.UP]: 'Działa',
      [StatusState.DEGRADED]: 'Działa z problemami',
      [StatusState.DOWN]: 'Nie działa',
      [StatusState.UNKNOWN]: 'Stan nieznany',
      [StatusState.MAINTENANCE]: 'Prace planowe',
    }

    for (const state of states) {
      const view = render(
        <AppThemeProvider>
          <StatusBadge state={state} />
        </AppThemeProvider>,
      )
      expect(screen.getByText(labels[state])).toBeDefined()
      expect(view.container.querySelectorAll('svg')).toHaveLength(1)
      view.unmount()
    }
  })
})
