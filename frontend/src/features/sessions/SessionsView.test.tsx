import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '../../app/i18n'
import { coreTokens } from '../../shared/theme'
import { renderWithProviders } from '../../test/providers'
import { installViewport } from '../../test/viewport'
import { SessionsView } from './components/SessionsView'
import { NewTokenSecretBanner } from './components/NewTokenSecretBanner'
import type { NewTokenSecretBannerProps, SessionData, SessionsViewProps } from './sessionTypes'
import { sessionsResources } from './translations'

const DESKTOP_WIDTH = 1024
const MOBILE_WIDTH = 375
const PIXELS_PER_REM = 16
const NOW = new Date('2026-11-25T12:00:00.000Z')
const SECRET = 'wk_live_7f3a9c_test_secret_value'

const sessions: readonly SessionData[] = [
  {
    id: 'current',
    device: 'Windows 11',
    browser: 'Chrome 131',
    lastActive: '2026-11-25T11:59:30.000Z',
    location: 'Warszawa, PL',
    current: true,
  },
  {
    id: 'mobile',
    device: 'Pixel 9',
    browser: 'Chrome 130',
    lastActive: '2026-11-24T21:10:00.000Z',
    location: 'Kraków, PL',
    current: false,
  },
  {
    id: 'linux',
    device: 'Linux',
    browser: 'Firefox 133',
    lastActive: '2026-11-12T10:00:00.000Z',
    location: 'Frankfurt, DE',
    current: false,
  },
]

function renderView(overrides: Partial<SessionsViewProps> = {}): SessionsViewProps {
  const props: SessionsViewProps = {
    sessions,
    status: 'ready',
    pendingIds: [],
    onLogout: vi.fn(),
    onLogoutAll: vi.fn(),
    onRetry: vi.fn(),
    now: NOW,
    ...overrides,
  }
  renderWithProviders(<SessionsView {...props} />)
  return props
}

function renderBanner(overrides: Partial<NewTokenSecretBannerProps> = {}): {
  onCopy: ReturnType<typeof vi.fn>
  onDismiss: ReturnType<typeof vi.fn>
} {
  const props: NewTokenSecretBannerProps = {
    name: 'ci-deploy',
    secret: SECRET,
    onCopy: vi.fn(),
    onDismiss: vi.fn(),
    ...overrides,
  }
  renderWithProviders(<NewTokenSecretBanner {...props} />)
  return {
    onCopy: props.onCopy as ReturnType<typeof vi.fn>,
    onDismiss: props.onDismiss as ReturnType<typeof vi.fn>,
  }
}

function rowFor(device: string): HTMLTableRowElement {
  const row = screen.getByText(device).closest<HTMLTableRowElement>('tr')
  if (row === null) {
    throw new Error(`No row for ${device}`)
  }
  return row
}

function openLogoutDialog(): void {
  fireEvent.click(screen.getByRole('button', { name: 'Wyloguj wszędzie' }))
}

function findLogoutDialog(): Promise<HTMLElement> {
  return screen.findByRole('dialog', { name: 'Wylogować ze wszystkich urządzeń?' })
}

beforeEach(async () => {
  i18n.addResourceBundle('pl', 'translation', sessionsResources.pl, true, true)
  i18n.addResourceBundle('en', 'translation', sessionsResources.en, true, true)
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

afterEach(async () => {
  cleanup()
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

describe('sessions view content', () => {
  it('the sessions view lists device, browser, last activity and location', () => {
    renderView()

    expect(screen.getByText('Windows 11')).toBeDefined()
    expect(screen.getByText('Chrome 131')).toBeDefined()
    expect(screen.getByText('Warszawa, PL')).toBeDefined()
    expect(screen.getByText('teraz')).toBeDefined()
    expect(screen.getByText('15 godzin temu')).toBeDefined()
    expect(screen.getByText('12.11.2026')).toBeDefined()
  })

  it('the current session is marked and has no logout action', () => {
    renderView()

    const current = rowFor('Windows 11')
    expect(within(current).getByText('to urządzenie')).toBeDefined()
    expect(within(current).queryByRole('button', { name: 'Wyloguj' })).toBeNull()

    const other = rowFor('Pixel 9')
    expect(within(other).getByRole('button', { name: 'Wyloguj' })).toBeDefined()
  })
})

describe('sessions logout', () => {
  it('logging out one session calls onLogout with its id', () => {
    const props = renderView()

    fireEvent.click(within(rowFor('Pixel 9')).getByRole('button', { name: 'Wyloguj' }))

    expect(props.onLogout).toHaveBeenCalledWith('mobile')
    expect(props.onLogout).toHaveBeenCalledTimes(1)
  })

  it('logout everywhere asks for confirmation and calls onLogoutAll once', async () => {
    const props = renderView()
    openLogoutDialog()

    const dialog = await findLogoutDialog()
    const confirm = within(dialog).getByRole('button', { name: 'Wyloguj wszędzie' })
    fireEvent.click(confirm)
    fireEvent.click(confirm)

    expect(props.onLogoutAll).toHaveBeenCalledTimes(1)
  })

  it('a pending session cannot be logged out twice', () => {
    const props = renderView({ pendingIds: ['mobile'] })
    const button = within(rowFor('Pixel 9')).getByRole('button', { name: 'Wyloguj' })

    expect(button.hasAttribute('disabled')).toBe(true)
    fireEvent.click(button)
    fireEvent.click(button)

    expect(props.onLogout).not.toHaveBeenCalled()
  })
})

describe('logout everywhere dialog', () => {
  it('the logout everywhere dialog says that API tokens stay valid', async () => {
    renderView()
    openLogoutDialog()

    const dialog = await findLogoutDialog()
    expect(within(dialog).getByText(/Tokeny API nie zostaną unieważnione/)).toBeDefined()
    expect(dialog.getAttribute('aria-modal')).toBe('true')
    expect(dialog.getAttribute('aria-labelledby')).not.toBeNull()
  })
})

describe('sessions view states', () => {
  it('the sessions view shows a skeleton while loading', () => {
    renderView({ status: 'loading', sessions: [] })

    expect(screen.getByRole('status', { name: 'Ładowanie sesji…' })).toBeDefined()
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('the sessions view explains the empty state', () => {
    renderView({ status: 'empty', sessions: [] })

    expect(screen.getByText('Brak aktywnych sesji')).toBeDefined()
    expect(
      screen.getByText('Gdy zalogujesz się na innym urządzeniu, zobaczysz je tutaj.'),
    ).toBeDefined()
  })

  it('the sessions view offers a retry after an error', () => {
    const props = renderView({ status: 'error', sessions: [] })

    expect(screen.getByText('Nie udało się wczytać sesji')).toBeDefined()
    fireEvent.click(screen.getByRole('button', { name: 'Spróbuj ponownie' }))
    expect(props.onRetry).toHaveBeenCalledTimes(1)
  })
})

describe('sessions view mobile', () => {
  it('on mobile the sessions are cards with touch targets of 44 px', () => {
    installViewport(MOBILE_WIDTH)
    renderView()

    expect(screen.getAllByRole('article')).toHaveLength(sessions.length)
    expect(screen.queryByRole('table')).toBeNull()

    const card = screen.getByRole('article', { name: 'Pixel 9' })
    const logout = within(card).getByRole('button', { name: 'Wyloguj' })
    const touchToken = coreTokens.shared['--app-size-control-touch'] ?? ''

    expect(Number.parseFloat(touchToken) * PIXELS_PER_REM).toBe(44)
    expect(logout.style.minHeight).toBe('var(--app-size-control-touch)')
  })
})

describe('new token secret banner', () => {
  it('the new token banner shows the secret once and hides it after dismissing', () => {
    const props = renderBanner()

    expect(screen.getByText(SECRET)).toBeDefined()
    expect(screen.getByText(/Nie pokażemy go ponownie/)).toBeDefined()

    fireEvent.click(screen.getByRole('button', { name: 'Zamknij' }))

    expect(screen.queryByText(SECRET)).toBeNull()
    expect(props.onDismiss).toHaveBeenCalledTimes(1)
  })

  it('the copy button reports through onCopy', () => {
    const props = renderBanner()

    fireEvent.click(screen.getByRole('button', { name: 'Kopiuj' }))

    expect(props.onCopy).toHaveBeenCalledTimes(1)
  })
})
