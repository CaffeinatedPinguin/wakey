import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactElement } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AppThemeProvider } from '../../../app/App'
import i18n from '../../../app/i18n'
import { installMockApi } from '../../../test/mockApi'
import type { MockRequest } from '../../../test/mockApi'
import { CreateSpaceDialog } from './CreateSpaceDialog'
import type { CreateSpaceDialogProps } from './CreateSpaceDialog'

function renderDialog(overrides: Partial<CreateSpaceDialogProps> = {}): {
  onCreated: ReturnType<typeof vi.fn>
} {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const onCreated = vi.fn()
  const props: CreateSpaceDialogProps = {
    opened: true,
    onClose: vi.fn(),
    onCreated,
    ...overrides,
  }
  const dialog: ReactElement = <CreateSpaceDialog {...props} />
  render(
    <AppThemeProvider env="test">
      <QueryClientProvider client={queryClient}>{dialog}</QueryClientProvider>
    </AppThemeProvider>,
  )
  return { onCreated: props.onCreated as ReturnType<typeof vi.fn> }
}

function nameInput(): HTMLInputElement {
  return screen.getByLabelText<HTMLInputElement>('Nazwa')
}

function slugInput(): HTMLInputElement {
  return screen.getByLabelText<HTMLInputElement>('Slug')
}

beforeEach(() => {
  installMockApi(() => ({ status: 500, data: { code: 'internal_error' } }))
})

afterEach(async () => {
  cleanup()
  await i18n.changeLanguage('pl')
})

describe('create space dialog slug', () => {
  it('the slug hint states the whole rule', () => {
    renderDialog()
    expect(
      screen.getByText(
        '3–40 znaków: małe litery, cyfry i myślniki; zaczyna się literą i nie kończy się myślnikiem.',
      ),
    ).toBeDefined()
  })

  it('the slug follows the name until it is edited by hand', () => {
    renderDialog()

    fireEvent.change(nameInput(), { target: { value: '123 Corp' } })
    expect(slugInput().value).toBe('')

    fireEvent.change(nameInput(), { target: { value: 'Mój Serwer' } })
    expect(slugInput().value).toBe('moj-serwer')

    fireEvent.change(slugInput(), { target: { value: 'wlasny-slug' } })
    fireEvent.change(nameInput(), { target: { value: 'Inna nazwa' } })

    expect(slugInput().value).toBe('wlasny-slug')
  })

  it('an invalid slug blocks submitting and shows a message', async () => {
    const { onCreated } = renderDialog()
    fireEvent.change(nameInput(), { target: { value: 'Produkcja' } })
    fireEvent.change(slugInput(), { target: { value: 'A' } })

    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(
      await screen.findByText(
        'Slug musi mieć 3–40 znaków: małe litery, cyfry i myślniki, zaczynać się literą i nie kończyć myślnikiem.',
      ),
    ).toBeDefined()
    expect(onCreated).not.toHaveBeenCalled()
  })
})

describe('create space dialog server errors', () => {
  it('a taken slug from the server is shown on the slug field', async () => {
    installMockApi(() => ({ status: 409, data: { code: 'slug_taken' } }))
    renderDialog()
    fireEvent.change(nameInput(), { target: { value: 'Alpha' } })

    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(await screen.findByText('Ten slug jest już zajęty.')).toBeDefined()
  })

  it('server validation errors are shown on the form fields', async () => {
    installMockApi(() => ({
      status: 400,
      data: {
        code: 'validation_failed',
        errors: [
          { code: 'invalid_name', pointer: '/name' },
          { code: 'invalid_slug', pointer: '/slug' },
        ],
      },
    }))
    renderDialog()
    fireEvent.change(nameInput(), { target: { value: 'Inna nazwa' } })
    fireEvent.change(slugInput(), { target: { value: 'inna-nazwa' } })

    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(await screen.findByText('Nazwa jest nieprawidłowa.')).toBeDefined()
    expect(
      screen.getByText(
        'Slug musi mieć 3–40 znaków: małe litery, cyfry i myślniki, zaczynać się literą i nie kończyć myślnikiem.',
      ),
    ).toBeDefined()
  })
})

describe('create space dialog submit', () => {
  it('submitting sends the trimmed name and the slug once', async () => {
    const requests: MockRequest[] = []
    installMockApi((request) => {
      requests.push(request)
      return { status: 201, data: { slug: 'produkcja', name: 'Produkcja', role: null, version: 1 } }
    })
    const { onCreated } = renderDialog()
    fireEvent.change(nameInput(), { target: { value: '  Produkcja  ' } })
    expect(slugInput().value).toBe('produkcja')

    const submitButton = screen.getByRole('button', { name: 'Utwórz' })
    fireEvent.click(submitButton)
    fireEvent.click(submitButton)

    await waitFor(() => {
      expect(onCreated).toHaveBeenCalledTimes(1)
    })
    expect(requests).toHaveLength(1)
    expect(onCreated).toHaveBeenCalledWith('produkcja')
  })
})
