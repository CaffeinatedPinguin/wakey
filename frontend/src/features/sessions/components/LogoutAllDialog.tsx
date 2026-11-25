import { Button, Group, Modal, Stack, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { LogoutAllDialogProps } from '../sessionTypes'

export function LogoutAllDialog(props: LogoutAllDialogProps): ReactElement {
  const { t } = useTranslation()
  const [pending, setPending] = useState(false)

  function close(): void {
    setPending(false)
    props.onClose()
  }

  function confirm(): void {
    if (!pending) {
      setPending(true)
      props.onConfirm()
    }
  }

  return (
    <Modal
      opened={props.opened}
      onClose={close}
      title={t('sessions.logoutAllDialog.title')}
      size="var(--app-size-modal-confirm)"
    >
      <Stack gap="sm">
        <Text size="sm">{t('sessions.logoutAllDialog.body', { total: props.sessionCount })}</Text>
        <Text size="sm" c="dimmed">
          {t('sessions.logoutAllDialog.hint')}
        </Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={close}>
            {t('sessions.logoutAllDialog.cancel')}
          </Button>
          <Button color="danger" loading={pending} onClick={confirm}>
            {t('sessions.logoutAllDialog.confirm')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
