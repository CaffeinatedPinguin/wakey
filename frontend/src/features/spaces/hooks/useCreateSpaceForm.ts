import { useForm } from '@mantine/form'
import type { UseFormReturnType } from '@mantine/form'
import type { ChangeEvent, ReactNode } from 'react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { spaceFieldErrors } from '../api/spaces.api'
import { useCreateSpace } from '../api/spaces.queries'
import { isValidSlug } from '../slug'
import type { SpaceFormValues, SpaceSubmitError } from '../spaceTypes'
import { createClose, createHandlers, createSubmit } from './createSpaceFormActions'
import { errorsOf, submitErrorOf } from './createSpaceErrors'

// replaced by the contract constant in the client task
const NAME_MAX_LENGTH = 100

export interface UseCreateSpaceFormOptions {
  opened: boolean
  onClose: () => void
  onCreated: (slug: string) => void
}

export interface CreateSpaceForm {
  form: UseFormReturnType<SpaceFormValues>
  nameError: ReactNode
  slugError: ReactNode
  submitError: SpaceSubmitError
  isPending: boolean
  close: () => void
  submit: (values: SpaceFormValues) => void
  onNameChange: (event: ChangeEvent<HTMLInputElement>) => void
  onSlugChange: (event: ChangeEvent<HTMLInputElement>) => void
}

function validateSpace(
  values: SpaceFormValues,
  t: (key: string) => string,
): Record<string, string> {
  const errors: Record<string, string> = {}
  const name = values.name.trim()
  if (name.length === 0) {
    errors.name = t('spaces.create.errors.nameRequired')
  }
  if (name.length > NAME_MAX_LENGTH) {
    errors.name = t('spaces.create.errors.nameTooLong')
  }
  if (!isValidSlug(values.slug)) {
    errors.slug = t('spaces.create.errors.slugInvalid')
  }
  return errors
}

export function useCreateSpaceForm({
  opened,
  onClose,
  onCreated,
}: UseCreateSpaceFormOptions): CreateSpaceForm {
  const { t } = useTranslation()
  const create = useCreateSpace()
  const [slugEdited, setSlugEdited] = useState(false)
  const form = useForm<SpaceFormValues>({
    initialValues: { name: '', slug: '' },
    validate: (values) => validateSpace(values, t),
  })
  const reset = create.reset
  useEffect(() => {
    if (opened) {
      reset()
    }
  }, [opened, reset])
  const fieldErrors = spaceFieldErrors(create.error)
  const submitError = submitErrorOf(create.error, fieldErrors)
  const { nameError, slugError } = errorsOf(form, fieldErrors, submitError, t)
  const handlers = createHandlers(form, slugEdited, setSlugEdited)
  const close = createClose(create, onClose)

  return {
    form,
    nameError,
    slugError,
    submitError,
    isPending: create.isPending,
    close,
    submit: createSubmit(create, close, onCreated),
    ...handlers,
  }
}
