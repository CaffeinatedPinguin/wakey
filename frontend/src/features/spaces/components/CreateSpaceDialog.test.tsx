import { cleanup, fireEvent, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import i18n from '../../../app/i18n'
import { renderWithProviders } from '../../../test/providers'
import { CreateSpaceDialog } from './CreateSpaceDialog'
import type { CreateSpaceDialogProps, SpaceSubmitError } from './CreateSpaceDialog'

function renderDialog(overrides: Partial<CreateSpaceDialogProps> = {}): {
  onSubmit: ReturnType<typeof vi.fn>
} {
  const onSubmit = vi.fn()
  const props: CreateSpaceDialogProps = {
    opened: true,
    submitting: false,
    submitError: null,
    onSubmit,
    onClose: vi.fn(),
    ...overrides,
  }
  renderWithProviders(<CreateSpaceDialog {...props} />)
  return { onSubmit: props.onSubmit as ReturnType<typeof vi.fn> }
}

function nameInput(): HTMLInputElement {
  return screen.getByLabelText<HTMLInputElement>('Nazwa')
}

function slugInput(): HTMLInputElement {
  return screen.getByLabelText<HTMLInputElement>('Slug')
}

afterEach(async () => {
  cleanup()
  await i18n.changeLanguage('pl')
})

describe('create space dialog slug', () => {
  it('the slug follows the name until it is edited by hand', () => {
    renderDialog()

    fireEvent.change(nameInput(), { target: { value: 'Mój Serwer' } })
    expect(slugInput().value).toBe('moj-serwer')

    fireEvent.change(slugInput(), { target: { value: 'wlasny-slug' } })
    fireEvent.change(nameInput(), { target: { value: 'Inna nazwa' } })

    expect(slugInput().value).toBe('wlasny-slug')
  })

  it('an invalid slug blocks submitting and shows a message', async () => {
    const { onSubmit } = renderDialog()
    fireEvent.change(nameInput(), { target: { value: 'Produkcja' } })
    fireEvent.change(slugInput(), { target: { value: 'A' } })

    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(
      await screen.findByText('Slug musi mieć 3–40 znaków: małe litery, cyfry i myślniki.'),
    ).toBeDefined()
    expect(onSubmit).not.toHaveBeenCalled()
  })
})

describe('create space dialog server errors', () => {
  it('a taken slug from the server is shown on the slug field', () => {
    const submitError: SpaceSubmitError = 'slug_taken'
    renderDialog({ submitError })

    expect(screen.getByText('Ten slug jest już zajęty.')).toBeDefined()
  })
})

describe('create space dialog submit', () => {
  it('submitting sends the trimmed name and the slug once', () => {
    const { onSubmit } = renderDialog()
    fireEvent.change(nameInput(), { target: { value: '  Produkcja  ' } })
    expect(slugInput().value).toBe('produkcja')

    const submitButton = screen.getByRole('button', { name: 'Utwórz' })
    fireEvent.click(submitButton)
    fireEvent.click(submitButton)

    expect(onSubmit).toHaveBeenCalledTimes(1)
    expect(onSubmit).toHaveBeenCalledWith({ name: 'Produkcja', slug: 'produkcja' })
  })
})
