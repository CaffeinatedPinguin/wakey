import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '../../../app/i18n'
import { coreTokens } from '../../../shared/theme'
import { renderWithProviders } from '../../../test/providers'
import { installViewport } from '../../../test/viewport'
import { SpaceSwitcher } from './SpaceSwitcher'
import type { SpaceSwitcherProps } from './SpaceSwitcher'

const DESKTOP_WIDTH = 1024
const MOBILE_WIDTH = 375
const PIXELS_PER_REM = 16

const spaces = [
  { slug: 'produkcja', name: 'Produkcja' },
  { slug: 'staging', name: 'Staging' },
]

function renderSwitcher(overrides: Partial<SpaceSwitcherProps> = {}): SpaceSwitcherProps {
  const props: SpaceSwitcherProps = {
    spaces,
    currentSlug: 'produkcja',
    status: 'ready',
    canCreate: true,
    hasMore: false,
    layout: 'menu',
    onSelect: vi.fn(),
    onCreate: vi.fn(),
    onRetry: vi.fn(),
    ...overrides,
  }
  renderWithProviders(<SpaceSwitcher {...props} />)
  return props
}

function openSwitcher(): void {
  fireEvent.click(screen.getByRole('button', { name: 'Przełącz przestrzeń' }))
}

beforeEach(() => {
  installViewport(DESKTOP_WIDTH)
})

afterEach(async () => {
  cleanup()
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

describe('space switcher desktop', () => {
  it('the space switcher lists spaces and marks the current one', async () => {
    renderSwitcher()
    openSwitcher()

    const current = await screen.findByRole('menuitem', { name: 'Produkcja' })
    const dropdown = current.parentElement ?? document.body
    const staging = within(dropdown).getByText('Staging')

    expect(current.getAttribute('aria-current')).toBe('true')
    expect(staging.closest('[role="menuitem"]')?.getAttribute('aria-current')).toBeNull()
  })

  it('choosing a space calls onSelect with its slug', async () => {
    const props = renderSwitcher()
    openSwitcher()

    fireEvent.click(await screen.findByRole('menuitem', { name: 'Staging' }))

    expect(props.onSelect).toHaveBeenCalledWith('staging')
    expect(props.onSelect).toHaveBeenCalledTimes(1)
  })

  it('the create action is hidden when the user cannot create spaces', async () => {
    renderSwitcher({ canCreate: false })
    openSwitcher()

    expect(await screen.findByRole('menuitem', { name: 'Produkcja' })).toBeDefined()
    const createAction = screen.queryByText('Utwórz przestrzeń')
    expect(createAction).toBeNull()
  })
})

describe('space switcher states', () => {
  it('the space switcher shows a skeleton while loading', async () => {
    renderSwitcher({ status: 'loading', spaces: [] })

    const trigger = screen.getByRole('button', { name: 'Przełącz przestrzeń' })
    expect(trigger.getAttribute('aria-busy')).toBe('true')
    fireEvent.click(trigger)

    expect(await screen.findByRole('status', { name: 'Ładowanie przestrzeni…' })).toBeDefined()
    expect(screen.queryByRole('menuitem')).toBeNull()
  })

  it('the space switcher explains the empty state differently for admins and others', async () => {
    renderSwitcher({ status: 'empty', spaces: [], canCreate: true })
    openSwitcher()
    const adminMessage = await screen.findByText(
      'Nie masz jeszcze żadnej przestrzeni. Utwórz pierwszą, żeby zacząć.',
    )
    expect(adminMessage).toBeDefined()
    expect(screen.getByText('Utwórz przestrzeń')).toBeDefined()

    cleanup()
    renderSwitcher({ status: 'empty', spaces: [], canCreate: false })
    openSwitcher()
    const memberMessage = await screen.findByText(
      'Nie masz dostępu do żadnej przestrzeni. Poproś administratora instancji o zaproszenie.',
    )
    expect(memberMessage).toBeDefined()
    const createAction = screen.queryByText('Utwórz przestrzeń')
    expect(createAction).toBeNull()
  })
})

describe('space switcher theme tokens', () => {
  it('the switcher sizes controls from theme tokens', async () => {
    renderSwitcher({ status: 'loading', spaces: [] })
    const trigger = screen.getByRole('button', { name: 'Przełącz przestrzeń' })
    const triggerSkeleton = trigger.querySelector<HTMLElement>('[style*="--skeleton-width"]')

    expect(triggerSkeleton?.style.getPropertyValue('--skeleton-width')).toContain('var(--app-')
    expect(triggerSkeleton?.style.getPropertyValue('--skeleton-height')).toContain('var(--app-')

    fireEvent.click(trigger)
    const loading = await screen.findByRole('status', { name: 'Ładowanie przestrzeni…' })
    const rows = loading.querySelectorAll<HTMLElement>('[style*="--skeleton-height"]')

    expect(rows.length).toBeGreaterThan(0)
    for (const row of rows) {
      expect(row.style.getPropertyValue('--skeleton-height')).toContain('var(--app-')
    }

    const dropdown = document.getElementById(trigger.getAttribute('aria-controls') ?? '')
    expect(dropdown?.style.width).toContain('var(--app-')
  })
})

describe('space switcher error', () => {
  it('the space switcher offers a retry after an error', async () => {
    const props = renderSwitcher({ status: 'error', spaces: [] })
    openSwitcher()

    fireEvent.click(await screen.findByRole('button', { name: 'Spróbuj ponownie' }))

    expect(props.onRetry).toHaveBeenCalledTimes(1)
  })
})

describe('space switcher pagination', () => {
  it('the switcher offers to load more spaces when there is another page', async () => {
    const onLoadMore = vi.fn()
    renderSwitcher({ layout: 'menu', hasMore: true, onLoadMore })
    openSwitcher()

    fireEvent.click(await screen.findByRole('button', { name: 'Wczytaj więcej' }))
    expect(onLoadMore).toHaveBeenCalledTimes(1)

    cleanup()
    renderSwitcher({ layout: 'sheet', hasMore: true, onLoadMore })
    openSwitcher()

    fireEvent.click(await screen.findByRole('button', { name: 'Wczytaj więcej' }))
    expect(onLoadMore).toHaveBeenCalledTimes(2)
  })
})

describe('space switcher mobile', () => {
  it('on mobile the switcher opens as a sheet with touch targets of 44 px', async () => {
    installViewport(MOBILE_WIDTH)
    renderSwitcher({ layout: 'sheet' })
    openSwitcher()

    const sheet = await screen.findByRole('dialog', { name: 'Przestrzeń' })
    const row = within(sheet).getByRole('button', { name: 'Produkcja' })
    const touchToken = coreTokens.shared['--app-size-control-touch'] ?? ''
    const touchPixels = Number.parseFloat(touchToken) * PIXELS_PER_REM

    expect(row.getAttribute('aria-current')).toBe('true')
    expect(touchPixels).toBe(44)
    expect(row.style.minHeight).toBe('var(--app-size-control-touch)')
  })
})
