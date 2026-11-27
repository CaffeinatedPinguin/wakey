import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import i18n from '../../../app/i18n'
import { ErrorCode } from '../../../shared/api/generated/enums'
import {
  CREATE_SPACE_REQUEST_NAME_MAX_LENGTH,
  CREATE_SPACE_REQUEST_SLUG_MAX_LENGTH,
  CREATE_SPACE_REQUEST_SLUG_MIN_LENGTH,
} from '../../../shared/api/generated/limits'
import { nameInput, renderCreateSpaceDialog, slugInput } from '../../../test/createSpaceDialog'
import { installMockApi } from '../../../test/mockApi'
import type { MockReply, MockRequest } from '../../../test/mockApi'

const SLUG_MIN = String(CREATE_SPACE_REQUEST_SLUG_MIN_LENGTH)
const SLUG_MAX = String(CREATE_SPACE_REQUEST_SLUG_MAX_LENGTH)
const NAME_MAX = String(CREATE_SPACE_REQUEST_NAME_MAX_LENGTH)
const SLUG_RULE = `${SLUG_MIN}–${SLUG_MAX} znaków`
const SLUG_HINT = `${SLUG_RULE}: małe litery, cyfry i myślniki; zaczyna się literą i nie kończy się myślnikiem.`
const SLUG_ERROR = `Slug musi mieć ${SLUG_RULE}: małe litery, cyfry i myślniki, zaczynać się literą i nie kończyć myślnikiem.`
const NAME_INVALID = `Nazwa musi mieć 1–${NAME_MAX} znaków bez znaków sterujących.`

beforeEach(() => {
  installMockApi(() => ({ status: 500, data: { code: ErrorCode.INTERNAL_ERROR } }))
})

afterEach(async () => {
  cleanup()
  await i18n.changeLanguage('pl')
})

describe('create space dialog slug', () => {
  it('the slug hint states the whole rule', () => {
    renderCreateSpaceDialog()
    expect(screen.getByText(SLUG_HINT)).toBeDefined()
  })

  it('the slug follows the name until it is edited by hand', () => {
    renderCreateSpaceDialog()

    fireEvent.change(nameInput(), { target: { value: '123 Corp' } })
    expect(slugInput().value).toBe('')

    fireEvent.change(nameInput(), { target: { value: 'Mój Serwer' } })
    expect(slugInput().value).toBe('moj-serwer')

    fireEvent.change(slugInput(), { target: { value: 'wlasny-slug' } })
    fireEvent.change(nameInput(), { target: { value: 'Inna nazwa' } })

    expect(slugInput().value).toBe('wlasny-slug')
  })

  it('an invalid slug blocks submitting and shows a message', async () => {
    const { onCreated } = renderCreateSpaceDialog()
    fireEvent.change(nameInput(), { target: { value: 'Produkcja' } })
    fireEvent.change(slugInput(), { target: { value: 'A' } })

    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(await screen.findByText(SLUG_ERROR)).toBeDefined()
    expect(onCreated).not.toHaveBeenCalled()
  })
})

describe('create space dialog reset', () => {
  it('reopening the create dialog starts with an empty form', async () => {
    const { rerender } = renderCreateSpaceDialog()
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
    const { rerender } = renderCreateSpaceDialog()
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
    const { onCreated, rerender } = renderCreateSpaceDialog()
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
    renderCreateSpaceDialog()
    fireEvent.change(nameInput(), { target: { value: 'Alpha' } })

    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(await screen.findByText('Ten slug jest już zajęty.')).toBeDefined()
  })

  it('a server error whose pointer matches no field shows the form message', async () => {
    installMockApi(() => ({
      status: 400,
      data: {
        code: ErrorCode.VALIDATION_FAILED,
        errors: [
          {
            pointer: '/missing',
            code: ErrorCode.INVALID_NAME,
            params: { max: CREATE_SPACE_REQUEST_NAME_MAX_LENGTH },
          },
        ],
      },
    }))
    renderCreateSpaceDialog()
    fireEvent.change(nameInput(), { target: { value: 'Alpha' } })

    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(await screen.findByText('Dane formularza są nieprawidłowe.')).toBeDefined()
    expect(screen.queryByText(NAME_INVALID)).toBeNull()
  })
})

describe('create space dialog server field params', () => {
  it('a server field error reaches the form with its own params', async () => {
    const serverMin = String(CREATE_SPACE_REQUEST_SLUG_MIN_LENGTH + 2)
    const serverMax = String(CREATE_SPACE_REQUEST_SLUG_MAX_LENGTH + 20)
    installMockApi(() => ({
      status: 400,
      data: {
        code: ErrorCode.VALIDATION_FAILED,
        errors: [
          {
            pointer: '/slug',
            code: ErrorCode.INVALID_SLUG,
            params: { min: serverMin, max: serverMax },
          },
        ],
      },
    }))
    renderCreateSpaceDialog()
    fireEvent.change(nameInput(), { target: { value: 'Alpha' } })
    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(
      await screen.findByText(
        `Slug musi mieć ${serverMin}–${serverMax} znaków: małe litery, cyfry i myślniki, zaczynać się literą i nie kończyć myślnikiem.`,
      ),
    ).toBeDefined()
  })
})

describe('create space dialog form message', () => {
  it('a new submit clears the previous form message', async () => {
    let reply: MockReply = { status: 500, data: { code: ErrorCode.INTERNAL_ERROR } }
    installMockApi(() => reply)
    renderCreateSpaceDialog()
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
          {
            code: ErrorCode.INVALID_NAME,
            pointer: '/name',
            params: { max: CREATE_SPACE_REQUEST_NAME_MAX_LENGTH },
          },
          {
            code: ErrorCode.INVALID_SLUG,
            pointer: '/slug',
            params: {
              min: CREATE_SPACE_REQUEST_SLUG_MIN_LENGTH,
              max: CREATE_SPACE_REQUEST_SLUG_MAX_LENGTH,
            },
          },
        ],
      },
    }))
    renderCreateSpaceDialog()
    fireEvent.change(nameInput(), { target: { value: 'Inna nazwa' } })
    fireEvent.change(slugInput(), { target: { value: 'inna-nazwa' } })

    fireEvent.click(screen.getByRole('button', { name: 'Utwórz' }))

    expect(await screen.findByText(NAME_INVALID)).toBeDefined()
    expect(screen.getByText(SLUG_ERROR)).toBeDefined()
  })
})

describe('create space dialog submit', () => {
  it('submitting sends the trimmed name and the slug once', async () => {
    const requests: MockRequest[] = []
    installMockApi((request) => {
      requests.push(request)
      return { status: 201, data: { slug: 'produkcja', name: 'Produkcja', role: null, version: 1 } }
    })
    const { onCreated } = renderCreateSpaceDialog()
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
