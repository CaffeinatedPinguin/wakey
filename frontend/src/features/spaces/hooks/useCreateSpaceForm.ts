import { useForm } from '@mantine/form'
import type { UseFormReturnType } from '@mantine/form'
import type { ChangeEvent } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { applyApiError } from '../../../shared/api/formErrors'
import { CREATE_SPACE_REQUEST_NAME_MAX_LENGTH } from '../../../shared/api/generated/limits'
import { useCreateSpace } from '../api/spaces.queries'
import { isValidSlug } from '../slug'
import type { SpaceFormValues } from '../spaceTypes'
import { createClose, createHandlers, createSubmit } from './createSpaceFormActions'

export interface UseCreateSpaceFormOptions {
  onClose: () => void
  onCreated: (slug: string) => void
}

export interface CreateSpaceForm {
  form: UseFormReturnType<SpaceFormValues>
  formError: string | null
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
  const nameLength = Array.from(name).length
  if (nameLength === 0) {
    errors.name = t('spaces.create.errors.nameRequired')
  }
  if (nameLength > CREATE_SPACE_REQUEST_NAME_MAX_LENGTH) {
    errors.name = t('spaces.create.errors.nameTooLong')
  }
  if (!isValidSlug(values.slug)) {
    errors.slug = t('spaces.create.errors.slugInvalid')
  }
  return errors
}

export function useCreateSpaceForm({
  onClose,
  onCreated,
}: UseCreateSpaceFormOptions): CreateSpaceForm {
  const { t } = useTranslation()
  const create = useCreateSpace()
  const [slugEdited, setSlugEdited] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const form = useForm<SpaceFormValues>({
    initialValues: { name: '', slug: '' },
    validate: (values) => validateSpace(values, t),
  })
  const resetForm = (): void => {
    create.reset()
    form.reset()
    form.clearErrors()
    setSlugEdited(false)
    setFormError(null)
  }
  const handlers = createHandlers(form, slugEdited, setSlugEdited)
  const close = createClose(resetForm, onClose)
  const onError = (error: unknown): void => {
    setFormError(applyApiError(form, error, t))
  }
  const submitValues = createSubmit(create, close, onCreated, onError)
  const submit = (values: SpaceFormValues): void => {
    setFormError(null)
    submitValues(values)
  }

  return {
    form,
    formError,
    isPending: create.isPending,
    close,
    submit,
    ...handlers,
  }
}
