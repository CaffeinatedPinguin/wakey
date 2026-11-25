import { Alert, Button, Group, Modal, Stack, TextInput } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { useCreateSpaceForm } from '../hooks/useCreateSpaceForm'
import type { CreateSpaceForm } from '../hooks/useCreateSpaceForm'

export interface CreateSpaceDialogProps {
  opened: boolean
  onClose: () => void
  onCreated: (slug: string) => void
}

function SpaceFormFields({ spaceForm }: { spaceForm: CreateSpaceForm }): ReactElement {
  const { t } = useTranslation()
  const { form } = spaceForm

  return (
    <>
      <TextInput
        label={t('spaces.create.name')}
        {...form.getInputProps('name')}
        onChange={spaceForm.onNameChange}
      />
      <TextInput
        label={t('spaces.create.slug')}
        description={t('spaces.create.slugHint')}
        {...form.getInputProps('slug')}
        onChange={spaceForm.onSlugChange}
      />
    </>
  )
}

function SpaceDialogActions({ isPending }: { isPending: boolean }): ReactElement {
  const { t } = useTranslation()

  return (
    <Group justify="flex-end">
      <Button variant="default" type="reset">
        {t('spaces.create.cancel')}
      </Button>
      <Button type="submit" loading={isPending}>
        {t('spaces.create.submit')}
      </Button>
    </Group>
  )
}

export function CreateSpaceDialog({
  opened,
  onClose,
  onCreated,
}: CreateSpaceDialogProps): ReactElement {
  const { t } = useTranslation()
  const spaceForm = useCreateSpaceForm({ onClose, onCreated })
  const { form } = spaceForm

  return (
    <Modal opened={opened} onClose={spaceForm.close} title={t('spaces.create.title')}>
      <form
        onSubmit={form.onSubmit(spaceForm.submit)}
        onReset={(event) => {
          event.preventDefault()
          spaceForm.close()
        }}
      >
        <Stack gap="md">
          {spaceForm.formError === null ? null : (
            <Alert color="danger">{spaceForm.formError}</Alert>
          )}
          <SpaceFormFields spaceForm={spaceForm} />
          <SpaceDialogActions isPending={spaceForm.isPending} />
        </Stack>
      </form>
    </Modal>
  )
}
