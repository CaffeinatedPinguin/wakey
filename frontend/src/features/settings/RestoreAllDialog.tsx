import { Button, Group, Modal, Stack, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'

export interface RestoreAllDialogProps {
  opened: boolean
  onClose: () => void
  onConfirm: () => void
}

export function RestoreAllDialog(props: RestoreAllDialogProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Modal
      opened={props.opened}
      onClose={props.onClose}
      title={t('settings.restoreAllTitle')}
      centered
    >
      <Stack gap="md">
        <Text size="sm">{t('settings.restoreAllBody')}</Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={props.onClose}>
            {t('settings.cancel')}
          </Button>
          <Button color="danger" onClick={props.onConfirm}>
            {t('settings.restoreAllConfirm')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
