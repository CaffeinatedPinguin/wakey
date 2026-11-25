import type { UseFormReturnType } from '@mantine/form'
import type { ReactNode } from 'react'
import { hasSpaceFieldErrors, isSlugTakenError } from '../api/spaces.api'
import type { SpaceFieldErrors, SpaceFormValues, SpaceSubmitError } from '../spaceTypes'

export function submitErrorOf(
  error: Error | null,
  fieldErrors: SpaceFieldErrors,
): SpaceSubmitError {
  if (hasSpaceFieldErrors(fieldErrors)) {
    return null
  }
  if (error === null) {
    return null
  }
  return isSlugTakenError(error) ? 'slug_taken' : 'generic'
}

function fieldErrorText(
  active: boolean,
  key: string,
  translate: (key: string) => string,
): string | undefined {
  return active ? translate(key) : undefined
}

export function errorsOf(
  form: UseFormReturnType<SpaceFormValues>,
  fieldErrors: SpaceFieldErrors,
  submitError: SpaceSubmitError,
  translate: (key: string) => string,
): { nameError: ReactNode; slugError: ReactNode } {
  const nameError =
    form.errors.name ??
    fieldErrorText(fieldErrors.name, 'spaces.create.errors.nameInvalid', translate)
  const slugTaken =
    submitError === 'slug_taken' ? translate('spaces.create.errors.slugTaken') : undefined
  const slugError =
    form.errors.slug ??
    fieldErrorText(fieldErrors.slug, 'spaces.create.errors.slugInvalid', translate) ??
    slugTaken
  return { nameError, slugError }
}
