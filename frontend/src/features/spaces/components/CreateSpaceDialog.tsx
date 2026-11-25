import { Alert, Button, Group, Modal, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import type { UseFormReturnType } from '@mantine/form'
import type { ChangeEvent, ReactElement, ReactNode } from 'react'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { SpaceFieldErrors } from '../spaceTypes'
import { isValidSlug, slugFromName } from '../slug'

export interface SpaceFormValues {
  name: string
  slug: string
}

export type SpaceSubmitError = 'slug_taken' | 'generic' | null

export interface CreateSpaceDialogProps {
  opened: boolean
  submitting: boolean
  submitError: SpaceSubmitError
  fieldErrors?: SpaceFieldErrors | undefined
  onSubmit: (values: SpaceFormValues) => void
  onClose: () => void
}

interface SpaceFormProps {
  submitting: boolean
  submitError: SpaceSubmitError
  fieldErrors?: SpaceFieldErrors | undefined
  onSubmit: (values: SpaceFormValues) => void
  onClose: () => void
}

interface SpaceFormController {
  form: UseFormReturnType<SpaceFormValues>
  nameError: ReactNode
  slugFieldError: ReactNode
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
  if (name.length > 100) {
    errors.name = t('spaces.create.errors.nameTooLong')
  }
  if (!isValidSlug(values.slug)) {
    errors.slug = t('spaces.create.errors.slugInvalid')
  }
  return errors
}

function hasFailure(props: SpaceFormProps): boolean {
  return (
    props.submitError !== null ||
    props.fieldErrors?.name === true ||
    props.fieldErrors?.slug === true
  )
}

function slugErrorOf(props: SpaceFormProps, t: (key: string) => string): string | undefined {
  return props.submitError === 'slug_taken' ? t('spaces.create.errors.slugTaken') : undefined
}

function fieldErrorText(
  active: boolean | undefined,
  key: string,
  translate: (key: string) => string,
): string | undefined {
  return active === true ? translate(key) : undefined
}

function fieldErrorsOf(
  props: SpaceFormProps,
  form: UseFormReturnType<SpaceFormValues>,
  t: (key: string) => string,
): { nameError: ReactNode; slugFieldError: ReactNode } {
  const nameError =
    form.errors.name ??
    fieldErrorText(props.fieldErrors?.name, 'spaces.create.errors.nameInvalid', t)
  const slugFieldError =
    form.errors.slug ??
    fieldErrorText(props.fieldErrors?.slug, 'spaces.create.errors.slugInvalid', t) ??
    slugErrorOf(props, t)
  return { nameError, slugFieldError }
}

function useSpaceForm(props: SpaceFormProps): SpaceFormController {
  const { t } = useTranslation()
  const [slugEdited, setSlugEdited] = useState(false)
  const submitted = useRef(false)
  const failed = hasFailure(props)
  const form = useForm<SpaceFormValues>({
    initialValues: { name: '', slug: '' },
    validate: (values) => validateSpace(values, t),
  })
  useEffect(() => {
    if (failed) {
      submitted.current = false
    }
  }, [failed])

  function onNameChange(event: ChangeEvent<HTMLInputElement>): void {
    const value = event.currentTarget.value
    submitted.current = false
    form.setFieldValue('name', value)
    if (!slugEdited) {
      form.setFieldValue('slug', slugFromName(value))
    }
  }

  function onSlugChange(event: ChangeEvent<HTMLInputElement>): void {
    submitted.current = false
    setSlugEdited(true)
    form.setFieldValue('slug', event.currentTarget.value)
  }
  function submit(values: SpaceFormValues): void {
    if (submitted.current) {
      return
    }
    submitted.current = true
    props.onSubmit({ name: values.name.trim(), slug: values.slug })
  }
  const { nameError, slugFieldError } = fieldErrorsOf(props, form, t)
  return { form, nameError, slugFieldError, submit, onNameChange, onSlugChange }
}

function SpaceDialogActions({ submitting }: { submitting: boolean }): ReactElement {
  const { t } = useTranslation()

  return (
    <Group justify="flex-end">
      <Button variant="default" type="reset">
        {t('spaces.create.cancel')}
      </Button>
      <Button type="submit" loading={submitting}>
        {t('spaces.create.submit')}
      </Button>
    </Group>
  )
}

function CreateSpaceForm(props: SpaceFormProps): ReactElement {
  const { t } = useTranslation()
  const { form, nameError, slugFieldError, submit, onNameChange, onSlugChange } =
    useSpaceForm(props)

  return (
    <form
      onSubmit={form.onSubmit(submit)}
      onReset={(event) => {
        event.preventDefault()
        props.onClose()
      }}
    >
      <Stack gap="md">
        {props.submitError === 'generic' ? (
          <Alert color="danger">{t('spaces.create.errors.generic')}</Alert>
        ) : null}
        <TextInput
          label={t('spaces.create.name')}
          {...form.getInputProps('name')}
          error={nameError}
          onChange={onNameChange}
        />
        <TextInput
          label={t('spaces.create.slug')}
          description={t('spaces.create.slugHint')}
          {...form.getInputProps('slug')}
          error={slugFieldError}
          onChange={onSlugChange}
        />
        <SpaceDialogActions submitting={props.submitting} />
      </Stack>
    </form>
  )
}

export function CreateSpaceDialog(props: CreateSpaceDialogProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Modal opened={props.opened} onClose={props.onClose} title={t('spaces.create.title')}>
      <CreateSpaceForm
        submitting={props.submitting}
        submitError={props.submitError}
        fieldErrors={props.fieldErrors}
        onSubmit={props.onSubmit}
        onClose={props.onClose}
      />
    </Modal>
  )
}
