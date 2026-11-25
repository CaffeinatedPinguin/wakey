import { Button, Group, Modal, Stack, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'

interface BulkDeleteDialogProps {
  opened: boolean
  count: number
  onCancel: () => void
  onConfirm: () => void
}

export function BulkDeleteDialog(props: BulkDeleteDialogProps): ReactElement {
  const { t } = useTranslation()
  return (
    <Modal
      opened={props.opened}
      onClose={props.onCancel}
      title={t('checks.page.delete.title')}
      centered
    >
      <Stack gap="md">
        <Text size="sm">{t('checks.page.delete.body', { count: props.count })}</Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={props.onCancel}>
            {t('checks.page.delete.cancel')}
          </Button>
          <Button color="danger" onClick={props.onConfirm}>
            {t('checks.page.delete.confirm')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
