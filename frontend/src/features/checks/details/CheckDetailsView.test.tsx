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

function renderView(overrides: Partial<CheckDetailsViewProps> = {}): CheckDetailsViewProps {
  const props: CheckDetailsViewProps = {
    data: checkDetailsFixture,
    status: 'success',
    notFound: false,
    onAction: vi.fn(),
    onRetry: vi.fn(),
    now: NOW,
    busy: false,
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
    for (const name of ['Wstrzymaj', 'Usuń']) {
      expect(screen.getByRole('button', { name })).toBeTruthy()
    }

    fireEvent.click(screen.getByRole('button', { name: 'Wstrzymaj' }))
    expect(props.onAction).toHaveBeenCalledWith('PAUSE')
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

describe('check details tabs and controls', () => {
  it('the details page shows only tabs that have data', () => {
    renderView()

    expect(screen.queryAllByRole('tab')).toHaveLength(0)
    expect(screen.queryByText('Do sprawdzenia konfiguracji')).toBeNull()
  })

  it('the check details show no actions that cannot work', () => {
    renderView()

    for (const name of ['Sprawdź teraz', 'Edytuj', 'Duplikuj']) {
      expect(screen.queryByRole('button', { name })).toBeNull()
    }
    expect(screen.queryByText('—')).toBeNull()
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

    expect(props.onAction).toHaveBeenCalledWith('DELETE')
    expect(props.onAction).toHaveBeenCalledTimes(1)
  })

  it('on mobile the actions are in a menu', async () => {
    installViewport(MOBILE_WIDTH)
    renderView()

    expect(screen.queryAllByRole('tab')).toHaveLength(0)
    expect(screen.queryByRole('button', { name: 'Sprawdź teraz' })).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: 'Akcje' }))
    expect(await screen.findByRole('menuitem', { name: 'Wstrzymaj' })).toBeTruthy()
    expect(screen.getByRole('menuitem', { name: 'Usuń' })).toBeTruthy()
  })
})

describe('check details states', () => {
  it('the check details show a skeleton while the query is pending', () => {
    renderView({ status: 'pending', data: null })

    const status = screen.getByRole('status', { name: 'Ładowanie checka…' })
    expect(status.querySelector('[style*="--skeleton-height"]')).not.toBeNull()
  })

  it('a missing check shows the not found view with a way back', () => {
    renderView({ status: 'error', notFound: true, data: null })

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
