import type { TFunction } from 'i18next'
import { toApiError } from './apiError'

const ERROR_KEY_PREFIX = 'errors.'
const GENERIC_ERROR_KEY = 'errors.generic'

export type FieldErrorMessages = Record<string, string>

// The part of a form the shared error mapping needs. Mantine's form fits it.
export interface ApiErrorForm {
  setErrors: (errors: FieldErrorMessages) => void
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

// Shows a server error on the form: `errors[]` goes to the fields it points at
// and the function returns null, because the fields carry the whole message.
// Without field errors the translated top-level code becomes the form message;
// a code without a translation falls back to the generic text.
export function applyApiError(form: ApiErrorForm, error: unknown, t: TFunction): string | null {
  const apiError = toApiError(error)
  if (apiError.errors.length === 0) {
    return translateCode(t, apiError.code, {})
  }
  const fieldErrors: FieldErrorMessages = {}
  for (const fieldError of apiError.errors) {
    const path = pointerToPath(fieldError.pointer)
    fieldErrors[path] = translateCode(t, fieldError.code, fieldError.params)
  }
  form.setErrors(fieldErrors)
  return null
}
