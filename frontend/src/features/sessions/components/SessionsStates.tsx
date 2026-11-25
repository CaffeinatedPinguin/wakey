import { Alert, Button, Center, Skeleton, Stack, Text } from '@mantine/core'
import { IconAlertCircle, IconInbox } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import type { SessionsErrorProps } from '../sessionTypes'

export function SessionsLoading(): ReactElement {
  const { t } = useTranslation()

  return (
    <Stack gap="sm" role="status" aria-label={t('sessions.states.loading')}>
      <Skeleton height="var(--app-size-control-desktop)" />
      <Skeleton height="var(--app-size-collection-row)" />
      <Skeleton height="var(--app-size-collection-row)" />
      <Skeleton height="var(--app-size-collection-row)" />
    </Stack>
  )
}

export function SessionsEmpty(): ReactElement {
  const { t } = useTranslation()

  return (
    <Center>
      <Stack gap="xs" align="center" role="status">
        <IconInbox size="var(--app-size-icon-lg)" />
        <Text fw={600}>{t('sessions.states.emptyTitle')}</Text>
        <Text size="sm" c="dimmed" ta="center">
          {t('sessions.states.emptyDescription')}
        </Text>
      </Stack>
    </Center>
  )
}

export function SessionsError({ onRetry }: SessionsErrorProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Alert
      color="neutral"
      icon={<IconAlertCircle size="var(--app-size-icon)" />}
      title={t('sessions.states.errorTitle')}
    >
      <Stack gap="sm" align="flex-start">
        <Text size="sm">{t('sessions.states.errorDescription')}</Text>
        <Button variant="default" onClick={onRetry}>
          {t('sessions.states.retry')}
        </Button>
      </Stack>
    </Alert>
  )
}
