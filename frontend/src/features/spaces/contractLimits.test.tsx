import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import type { ReactElement } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AppThemeProvider } from '../../app/App'
import { CreateSpaceDialog } from './components/CreateSpaceDialog'

const alternateLimits = vi.hoisted(() => ({
  name: 120,
  slugMin: 5,
  slugMax: 60,
  pattern: '^[a-z][a-z0-9-]+[a-z0-9]$',
}))

vi.mock('../../shared/api/generated/limits', () => ({
  CREATE_SPACE_REQUEST_NAME_MAX_LENGTH: alternateLimits.name,
  CREATE_SPACE_REQUEST_SLUG_MAX_LENGTH: alternateLimits.slugMax,
  CREATE_SPACE_REQUEST_SLUG_MIN_LENGTH: alternateLimits.slugMin,
  CREATE_SPACE_REQUEST_SLUG_PATTERN: alternateLimits.pattern,
}))

const SLUG_MIN = String(alternateLimits.slugMin)
const SLUG_MAX = String(alternateLimits.slugMax)
const NAME_MAX = String(alternateLimits.name)

function renderDialog(): void {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const tree = (): ReactElement => (
    <AppThemeProvider env="test">
      <QueryClientProvider client={queryClient}>
        <CreateSpaceDialog opened onClose={vi.fn()} onCreated={vi.fn()} />
      </QueryClientProvider>
    </AppThemeProvider>
  )
  render(tree())
}

function nameInput(): HTMLInputElement {
  return screen.getByLabelText<HTMLInputElement>('Nazwa')
}

function slugInput(): HTMLInputElement {
  return screen.getByLabelText<HTMLInputElement>('Slug')
}

afterEach(cleanup)

describe('space limits from the contract', () => {
  it('the slug and name limits come from the contract', async () => {
    renderDialog()
    const slugRule = `${SLUG_MIN}–${SLUG_MAX} znaków`

    expect(
      screen.getByText(
        `${slugRule}: małe litery, cyfry i myślniki; zaczyna się literą i nie kończy się myślnikiem.`,
      ),
    ).toBeDefined()

    fireEvent.change(nameInput(), {
      target: { value: 'x'.repeat(alternateLimits.name + 1) },
    })
    fireEvent.change(slugInput(), { target: { value: 'valid-slug' } })
    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(
      await screen.findByText(`Nazwa musi mieć 1–${NAME_MAX} znaków bez znaków sterujących.`),
    ).toBeDefined()
  })
})
