import { useForm } from '@mantine/form'
import type { UseFormReturnType } from '@mantine/form'
import type { TFunction } from 'i18next'
import type { ChangeEvent } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiException, toApiException } from '../../../shared/api/apiException'
import { applyApiException } from '../../../shared/api/formErrors'
import { ErrorCode } from '../../../shared/api/generated/enums'
import {
  CREATE_SPACE_REQUEST_NAME_MAX_LENGTH,
  CREATE_SPACE_REQUEST_SLUG_MAX_LENGTH,
  CREATE_SPACE_REQUEST_SLUG_MIN_LENGTH,
} from '../../../shared/api/generated/limits'
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

const SLUG_LIMITS = {
  min: CREATE_SPACE_REQUEST_SLUG_MIN_LENGTH,
  max: CREATE_SPACE_REQUEST_SLUG_MAX_LENGTH,
}

// The server copies the constraint attributes into `params`; an error body that
// omits them still shows the slug rule from the contract instead of a gap.
function withSlugLimits(error: unknown): ApiException {
  const exception = toApiException(error)
  const errors = exception.errors.map((fieldError) =>
    fieldError.code === ErrorCode.INVALID_SLUG
      ? { ...fieldError, params: { ...SLUG_LIMITS, ...fieldError.params } }
      : fieldError,
  )
  return new ApiException({
    status: exception.status,
    code: exception.code,
    errors,
    detail: exception.detail,
  })
}

function validateSpace(values: SpaceFormValues, t: TFunction): Record<string, string> {
  const errors: Record<string, string> = {}
  const name = values.name.trim()
  const nameLength = Array.from(name).length
  if (nameLength === 0) {
    errors.name = t('spaces.create.errors.nameRequired')
  }
  if (nameLength > CREATE_SPACE_REQUEST_NAME_MAX_LENGTH) {
    errors.name = t('spaces.create.errors.nameTooLong', {
      max: CREATE_SPACE_REQUEST_NAME_MAX_LENGTH,
    })
  }
  if (!isValidSlug(values.slug)) {
    errors.slug = t('spaces.create.errors.slugInvalid', {
      min: CREATE_SPACE_REQUEST_SLUG_MIN_LENGTH,
      max: CREATE_SPACE_REQUEST_SLUG_MAX_LENGTH,
    })
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
    setFormError(applyApiException(form, withSlugLimits(error), t))
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
