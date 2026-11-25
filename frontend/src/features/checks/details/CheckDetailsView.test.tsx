import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '../../../app/i18n'
import { checkDetailsFixture, NOW, unknownCheckFixture } from '../../../test/checkDetailsData'
import { renderWithProviders } from '../../../test/providers'
import { installViewport } from '../../../test/viewport'
import { CheckDetailsView } from './CheckDetailsView'
import type { CheckDetailsViewProps } from './checkDetailsTypes'
import { checkDetailsResources } from './translations'

const DESKTOP_WIDTH = 1024
const MOBILE_WIDTH = 375

const TAB_LABELS = [
  'Zdarzenia',
  'Lokalizacje',
  'Powiadomienia',
  'Konfiguracja',
  'Certyfikat',
  'Historia zmian',
]

function renderView(overrides: Partial<CheckDetailsViewProps> = {}): CheckDetailsViewProps {
  const props: CheckDetailsViewProps = {
    data: checkDetailsFixture,
    status: 'ready',
    activeTab: 'events',
    onTabChange: vi.fn(),
    onAction: vi.fn(),
    onRetry: vi.fn(),
    now: NOW,
    ...overrides,
  }
  renderWithProviders(<CheckDetailsView {...props} />)
  return props
}

beforeAll(() => {
  i18n.addResourceBundle('pl', 'translation', checkDetailsResources.pl, true, true)
  i18n.addResourceBundle('en', 'translation', checkDetailsResources.en, true, true)
})

beforeEach(() => {
  installViewport(DESKTOP_WIDTH)
})

afterEach(async () => {
  cleanup()
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

describe('check details header', () => {
  it('the check header shows the name, target, state badge and actions', () => {
    const props = renderView()

    expect(screen.getByRole('heading', { name: 'API płatności' })).toBeTruthy()
    expect(screen.getByText('https://payments.example/health')).toBeTruthy()
    expect(screen.getAllByText('Nie działa').length).toBeGreaterThan(0)
    for (const name of ['Sprawdź teraz', 'Wstrzymaj', 'Edytuj', 'Duplikuj', 'Usuń']) {
      expect(screen.getByRole('button', { name })).toBeTruthy()
    }

    fireEvent.click(screen.getByRole('button', { name: 'Sprawdź teraz' }))
    expect(props.onAction).toHaveBeenCalledWith('check-now')

    fireEvent.click(screen.getByRole('tab', { name: 'Certyfikat' }))
    expect(props.onTabChange).toHaveBeenCalledWith('certificate')
  })

  it('a paused check offers resume instead of pause', () => {
    renderView({ data: { ...checkDetailsFixture, paused: true } })

    expect(screen.getByRole('button', { name: 'Wznów' })).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Wstrzymaj' })).toBeNull()
  })

  it('an unknown state shows the age of the data', () => {
    renderView({ data: unknownCheckFixture })

    expect(screen.getAllByText('Stan nieznany').length).toBeGreaterThan(0)
    expect(screen.getByText(/5 minut temu/)).toBeTruthy()
  })
})

describe('check details tabs', () => {
  it('the config hint is shown on every tab when there are reasons', () => {
    renderView()
    for (const label of TAB_LABELS) {
      fireEvent.click(screen.getByRole('tab', { name: label }))
      expect(screen.getByText('Do sprawdzenia konfiguracji')).toBeTruthy()
    }
    expect(screen.getByText(checkDetailsFixture.configIssues[0] ?? '')).toBeTruthy()
  })

  it('the events tab lists time, state and error category', () => {
    renderView({ activeTab: 'events' })

    expect(screen.getByText('timeout')).toBeTruthy()
    expect(screen.getByText('Warszawa · HTTP 503')).toBeTruthy()
    expect(screen.getAllByText('Nie działa').length).toBeGreaterThan(0)
    expect(screen.getAllByText(/\d{2}:\d{2}/).length).toBeGreaterThan(0)
  })

  it('the certificate tab shows issuer, expiry and days left', () => {
    renderView({ activeTab: 'certificate' })

    expect(screen.getByText("R11 · Let's Encrypt")).toBeTruthy()
    expect(screen.getByText('2027-02-01')).toBeTruthy()
    expect(screen.getByText('68')).toBeTruthy()
    expect(screen.getByText('AB:CD:EF:01')).toBeTruthy()
  })

  it('the change history shows who, when, what and the source', () => {
    renderView({ activeTab: 'history' })

    expect(screen.getByText('Anna K.')).toBeTruthy()
    expect(screen.getByText('Interwał 60 s → 300 s')).toBeTruthy()
    expect(screen.getByText('Plik')).toBeTruthy()
    expect(screen.getAllByText(/\d{2}\.\d{2}\.\d{4}/).length).toBeGreaterThan(0)
  })
})

describe('check details actions', () => {
  it('deleting a check asks for confirmation and calls onAction once', async () => {
    const props = renderView()

    fireEvent.click(screen.getByRole('button', { name: 'Usuń' }))
    const dialog = await screen.findByRole('dialog')

    expect(dialog.getAttribute('aria-modal')).toBe('true')
    expect(props.onAction).not.toHaveBeenCalled()

    const confirm = within(dialog).getByRole('button', { name: 'Usuń' })
    fireEvent.click(confirm)
    fireEvent.click(confirm)

    expect(props.onAction).toHaveBeenCalledWith('delete')
    expect(props.onAction).toHaveBeenCalledTimes(1)
  })

  it('on mobile there are four tabs and the actions are in a menu', async () => {
    installViewport(MOBILE_WIDTH)
    renderView()

    expect(screen.getAllByRole('tab')).toHaveLength(4)
    expect(screen.queryByRole('button', { name: 'Sprawdź teraz' })).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: 'Akcje' }))
    expect(await screen.findByRole('menuitem', { name: 'Sprawdź teraz' })).toBeTruthy()
  })
})

describe('check details states', () => {
  it('the check details show a skeleton while loading', () => {
    renderView({ status: 'loading', data: null })

    const status = screen.getByRole('status', { name: 'Ładowanie checka…' })
    expect(status.querySelector('[style*="--skeleton-height"]')).not.toBeNull()
  })

  it('a missing check shows the not found view with a way back', () => {
    renderView({ status: 'notFound', data: null })

    expect(screen.getByRole('heading', { name: 'Nie znaleziono checka' })).toBeTruthy()
    const backLink = screen.getByRole('link', { name: 'Wróć do listy checków' })
    expect(backLink.getAttribute('href')).toBe('/checks')
  })

  it('the check details offer a retry after an error', () => {
    const props = renderView({ status: 'error', data: null })

    fireEvent.click(screen.getByRole('button', { name: 'Spróbuj ponownie' }))
    expect(props.onRetry).toHaveBeenCalledTimes(1)
  })
})
