import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactElement } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AppThemeProvider } from '../../../app/App'
import i18n from '../../../app/i18n'
import { ErrorCode } from '../../../shared/api/generated/enums'
import { installMockApi } from '../../../test/mockApi'
import type { MockReply, MockRequest } from '../../../test/mockApi'
import { CreateSpaceDialog } from './CreateSpaceDialog'
import type { CreateSpaceDialogProps } from './CreateSpaceDialog'

function renderDialog(overrides: Partial<CreateSpaceDialogProps> = {}): {
  onCreated: ReturnType<typeof vi.fn>
  rerender: (opened: boolean) => void
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
  const tree = (opened: boolean): ReactElement => (
    <AppThemeProvider env="test">
      <QueryClientProvider client={queryClient}>
        <CreateSpaceDialog {...props} opened={opened} />
      </QueryClientProvider>
    </AppThemeProvider>
  )
  const { rerender } = render(tree(true))
  return {
    onCreated: props.onCreated as ReturnType<typeof vi.fn>,
    rerender: (opened) => {
      rerender(tree(opened))
    },
  }
}

function nameInput(): HTMLInputElement {
  return screen.getByLabelText<HTMLInputElement>('Nazwa')
}

function slugInput(): HTMLInputElement {
  return screen.getByLabelText<HTMLInputElement>('Slug')
}

beforeEach(() => {
  installMockApi(() => ({ status: 500, data: { code: ErrorCode.INTERNAL_ERROR } }))
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

describe('create space dialog reset', () => {
  it('reopening the create dialog starts with an empty form', async () => {
    const { rerender } = renderDialog()
    fireEvent.change(nameInput(), { target: { value: 'Produkcja' } })
    expect(slugInput().value).toBe('produkcja')

    fireEvent.click(screen.getByRole('button', { name: 'Anuluj' }))
    rerender(false)
    rerender(true)

    const name = await screen.findByLabelText<HTMLInputElement>('Nazwa')
    expect(name.value).toBe('')
    expect(screen.getByLabelText<HTMLInputElement>('Slug').value).toBe('')
  })

  it('the slug follows the name again after the dialog is reopened', async () => {
    const { rerender } = renderDialog()
    fireEvent.change(nameInput(), { target: { value: 'Produkcja' } })
    fireEvent.change(slugInput(), { target: { value: 'wlasny-slug' } })
    fireEvent.change(nameInput(), { target: { value: 'Inna nazwa' } })
    expect(slugInput().value).toBe('wlasny-slug')

    fireEvent.click(screen.getByRole('button', { name: 'Anuluj' }))
    rerender(false)
    rerender(true)
    fireEvent.change(await screen.findByLabelText('Nazwa'), { target: { value: 'Nowa nazwa' } })

    expect(screen.getByLabelText<HTMLInputElement>('Slug').value).toBe('nowa-nazwa')
  })
})

describe('create space dialog reset after creation', () => {
  it('after creating a space the dialog opens empty again', async () => {
    installMockApi(() => ({
      status: 201,
      data: { slug: 'produkcja', name: 'Produkcja', role: null, version: 1 },
    }))
    const { onCreated, rerender } = renderDialog()
    fireEvent.change(nameInput(), { target: { value: 'Produkcja' } })
    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))
    await waitFor(() => {
      expect(onCreated).toHaveBeenCalledWith('produkcja')
    })

    rerender(false)
    rerender(true)

    const name = await screen.findByLabelText<HTMLInputElement>('Nazwa')
    expect(name.value).toBe('')
    expect(screen.getByLabelText<HTMLInputElement>('Slug').value).toBe('')
  })
})

describe('create space dialog server errors', () => {
  it('a taken slug from the server is shown on the slug field', async () => {
    installMockApi(() => ({
      status: 409,
      data: {
        code: ErrorCode.SLUG_TAKEN,
        errors: [{ pointer: '/slug', code: ErrorCode.SLUG_TAKEN }],
      },
    }))
    renderDialog()
    fireEvent.change(nameInput(), { target: { value: 'Alpha' } })

    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(await screen.findByText('Ten slug jest już zajęty.')).toBeDefined()
  })

  it('a server error whose pointer matches no field shows the form message', async () => {
    installMockApi(() => ({
      status: 400,
      data: {
        code: ErrorCode.VALIDATION_FAILED,
        errors: [{ pointer: '/missing', code: ErrorCode.INVALID_NAME }],
      },
    }))
    renderDialog()
    fireEvent.change(nameInput(), { target: { value: 'Alpha' } })

    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(await screen.findByText('Dane formularza są nieprawidłowe.')).toBeDefined()
    expect(screen.queryByText('Nazwa musi mieć 1–100 znaków bez znaków sterujących.')).toBeNull()
  })
})

describe('create space dialog form message', () => {
  it('a new submit clears the previous form message', async () => {
    let reply: MockReply = { status: 500, data: { code: ErrorCode.INTERNAL_ERROR } }
    installMockApi(() => reply)
    renderDialog()
    fireEvent.change(nameInput(), { target: { value: 'Alpha' } })
    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))
    expect(await screen.findByText('Błąd serwera. Spróbuj ponownie.')).toBeDefined()

    reply = {
      status: 400,
      data: {
        code: ErrorCode.VALIDATION_FAILED,
        errors: [{ pointer: '/slug', code: ErrorCode.SLUG_TAKEN }],
      },
    }
    fireEvent.change(nameInput(), { target: { value: 'Beta' } })
    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    await waitFor(() => {
      expect(screen.queryByText('Błąd serwera. Spróbuj ponownie.')).toBeNull()
    })
    expect(await screen.findByText('Ten slug jest już zajęty.')).toBeDefined()
  })
})

describe('create space dialog server validation', () => {
  it('server validation errors are shown on the form fields', async () => {
    installMockApi(() => ({
      status: 400,
      data: {
        code: ErrorCode.VALIDATION_FAILED,
        errors: [
          { code: ErrorCode.INVALID_NAME, pointer: '/name', params: { max: 100 } },
          { code: ErrorCode.INVALID_SLUG, pointer: '/slug' },
        ],
      },
    }))
    renderDialog()
    fireEvent.change(nameInput(), { target: { value: 'Inna nazwa' } })
    fireEvent.change(slugInput(), { target: { value: 'inna-nazwa' } })

    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(
      await screen.findByText('Nazwa musi mieć 1–100 znaków bez znaków sterujących.'),
    ).toBeDefined()
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
