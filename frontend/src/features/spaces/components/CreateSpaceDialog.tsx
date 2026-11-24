import { Alert, Button, Group, Modal, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import type { UseFormReturnType } from '@mantine/form'
import type { ChangeEvent, ReactElement } from 'react'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
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
  onSubmit: (values: SpaceFormValues) => void
  onClose: () => void
}

interface SpaceFormProps {
  submitting: boolean
  submitError: SpaceSubmitError
  onSubmit: (values: SpaceFormValues) => void
  onClose: () => void
}

interface SpaceFormController {
  form: UseFormReturnType<SpaceFormValues>
  slugError: string | undefined
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

function useSpaceForm(props: SpaceFormProps): SpaceFormController {
  const { t } = useTranslation()
  const [slugEdited, setSlugEdited] = useState(false)
  const submitted = useRef(false)
  const form = useForm<SpaceFormValues>({
    initialValues: { name: '', slug: '' },
    validate: (values) => validateSpace(values, t),
  })
  useEffect(() => {
    if (props.submitError !== null) {
      submitted.current = false
    }
  }, [props.submitError])

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

  const slugError =
    props.submitError === 'slug_taken' ? t('spaces.create.errors.slugTaken') : undefined
  return { form, slugError, submit, onNameChange, onSlugChange }
}

function CreateSpaceForm(props: SpaceFormProps): ReactElement {
  const { t } = useTranslation()
  const { form, slugError, submit, onNameChange, onSlugChange } = useSpaceForm(props)

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
          onChange={onNameChange}
        />
        <TextInput
          label={t('spaces.create.slug')}
          description={t('spaces.create.slugHint')}
          {...form.getInputProps('slug')}
          error={form.errors.slug ?? slugError}
          onChange={onSlugChange}
        />
        <Group justify="flex-end">
          <Button variant="default" type="reset">
            {t('spaces.create.cancel')}
          </Button>
          <Button type="submit" loading={props.submitting}>
            {t('spaces.create.submit')}
          </Button>
        </Group>
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
        onSubmit={props.onSubmit}
        onClose={props.onClose}
      />
    </Modal>
  )
}
