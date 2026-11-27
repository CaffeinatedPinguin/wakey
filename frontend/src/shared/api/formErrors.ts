import type { TFunction } from 'i18next'
import { toApiException } from './apiException'

const ERROR_KEY_PREFIX = 'errors.'
const GENERIC_ERROR_KEY = 'errors.generic'

export type FieldErrorMessages = Record<string, string>

// The part of a form the shared error mapping needs. Mantine's form fits it.
export interface ApiExceptionForm {
  setErrors: (errors: FieldErrorMessages) => void
  getValues: () => object
}

// A JSON Pointer from the contract (`/slug`, `/a/b`) becomes the form path
// Mantine expects (`slug`, `a.b`).
export function pointerToPath(pointer: string): string {
  return pointer.slice(1).replaceAll('/', '.')
}

function translateCode(t: TFunction, code: string, params: Record<string, unknown>): string {
  const generic = t(GENERIC_ERROR_KEY)
  return t(`${ERROR_KEY_PREFIX}${code}`, { ...params, defaultValue: generic })
}

// Translates the top-level code of a server error through the shared vocabulary,
// so a screen without a form still shows the cause the server reported.
export function apiErrorMessage(t: TFunction, error: unknown): string {
  return translateCode(t, toApiException(error).code, {})
}

// Shows a server error on the form: `errors[]` goes to the fields it points at
// and the function returns null, because the fields carry the whole message.
// A pointer to no form field must not swallow the error, so the translated
// top-level code becomes the form message instead; a code without a translation
// falls back to the generic text.
export function applyApiException(
  form: ApiExceptionForm,
  error: unknown,
  t: TFunction,
): string | null {
  const apiException = toApiException(error)
  if (apiException.errors.length === 0) {
    return translateCode(t, apiException.code, {})
  }
  const formPaths = new Set(Object.keys(form.getValues()))
  const fieldErrors: FieldErrorMessages = {}
  for (const fieldError of apiException.errors) {
    const path = pointerToPath(fieldError.pointer)
    if (formPaths.has(path)) {
      fieldErrors[path] = translateCode(t, fieldError.code, fieldError.params)
    }
  }
  if (Object.keys(fieldErrors).length === 0) {
    return translateCode(t, apiException.code, {})
  }
  form.setErrors(fieldErrors)
  return null
}
