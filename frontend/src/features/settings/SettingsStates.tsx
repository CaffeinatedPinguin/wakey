import { Alert, Button, Skeleton, Stack, Text } from '@mantine/core'
import { IconAlertTriangle, IconInbox, IconRefresh } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'

export function SettingsLoading(): ReactElement {
  const { t } = useTranslation()

  return (
    <Stack gap="sm" role="status" aria-label={t('settings.loading')}>
      <Skeleton height="var(--app-type-metadata-line)" width="40%" />
      <Skeleton height="var(--app-size-row)" />
      <Skeleton height="var(--app-size-row)" />
      <Skeleton height="var(--app-size-row)" />
    </Stack>
  )
}

export function SettingsError({ onRetry }: { onRetry: () => void }): ReactElement {
  const { t } = useTranslation()

  return (
    <Alert color="danger" title={t('settings.error')} icon={<IconAlertTriangle size={16} />}>
      <Stack gap="sm" align="flex-start">
        <Button
          variant="light"
          leftSection={<IconRefresh size={16} />}
          onClick={onRetry}
          style={{ minHeight: 'var(--app-size-control-touch)' }}
        >
          {t('settings.retry')}
        </Button>
      </Stack>
    </Alert>
  )
}

export function SettingsEmpty(): ReactElement {
  const { t } = useTranslation()

  return (
    <Stack align="center" gap="sm" py="xl">
      <IconInbox size="var(--app-size-control-large)" aria-hidden />
      <Text c="dimmed" ta="center">
        {t('settings.empty')}
      </Text>
    </Stack>
  )
}
