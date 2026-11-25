import { Alert, Button, Code, Group, Stack, Text } from '@mantine/core'
import { IconAlertTriangle } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { NewTokenSecretBannerProps } from '../sessionTypes'

export function NewTokenSecretBanner(props: NewTokenSecretBannerProps): ReactElement | null {
  const { t } = useTranslation()
  const [visible, setVisible] = useState(true)

  if (!visible) {
    return null
  }

  function dismiss(): void {
    setVisible(false)
    props.onDismiss()
  }

  return (
    <Alert
      color="warning"
      icon={<IconAlertTriangle size="var(--app-size-icon)" />}
      title={t('sessions.newToken.title', { name: props.name })}
    >
      <Stack gap="sm">
        <Text size="sm">{t('sessions.newToken.warning')}</Text>
        <Code block>{props.secret}</Code>
        <Group justify="space-between">
          <Button variant="default" onClick={props.onCopy}>
            {t('sessions.newToken.copy')}
          </Button>
          <Button variant="subtle" onClick={dismiss}>
            {t('sessions.newToken.dismiss')}
          </Button>
        </Group>
      </Stack>
    </Alert>
  )
}
