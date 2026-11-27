import { Anchor, Button, Skeleton, Stack, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { PageFrame } from '../../../shared/ui'

export function CheckDetailsLoading(): ReactElement {
  const { t } = useTranslation()
  return (
    <PageFrame title={t('checks.details.title')}>
      <Stack gap="sm" role="status" aria-busy="true" aria-label={t('checks.details.loading')}>
        <Skeleton height="var(--app-type-page-title-line)" width="40%" />
        <Skeleton height="var(--app-type-body-line)" width="70%" />
        <Skeleton height="var(--app-type-body-line)" width="60%" />
        <Skeleton height="var(--app-size-control-touch)" />
      </Stack>
    </PageFrame>
  )
}

export function CheckDetailsMissingSpace(): ReactElement {
  const { t } = useTranslation()
  return (
    <PageFrame title={t('checks.details.missingSpace.title')}>
      <Stack gap="md">
        <Text>{t('checks.details.missingSpace.body')}</Text>
        <Anchor href="/checks">{t('checks.details.missingSpace.back')}</Anchor>
      </Stack>
    </PageFrame>
  )
}

export function CheckDetailsNotFound(): ReactElement {
  const { t } = useTranslation()
  return (
    <PageFrame title={t('checks.details.notFound.title')}>
      <Stack gap="md">
        <Text>{t('checks.details.notFound.body')}</Text>
        <Anchor href="/checks">{t('checks.details.notFound.back')}</Anchor>
      </Stack>
    </PageFrame>
  )
}

interface CheckDetailsErrorProps {
  readonly onRetry: () => void
}

export function CheckDetailsError(props: CheckDetailsErrorProps): ReactElement {
  const { t } = useTranslation()
  return (
    <PageFrame title={t('checks.details.error.title')}>
      <Stack gap="md">
        <Text>{t('checks.details.error.body')}</Text>
        <Button w="fit-content" onClick={props.onRetry}>
          {t('checks.details.retry')}
        </Button>
      </Stack>
    </PageFrame>
  )
}
