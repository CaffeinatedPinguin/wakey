import { Button, Group, Skeleton, Stack, Text } from '@mantine/core'
import { IconAlertTriangle, IconPlus, IconSearch } from '@tabler/icons-react'
import { Link } from '@tanstack/react-router'
import type { ReactElement, ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { PageFrame } from '../../../shared/ui'
import { touchStyle } from '../../../shared/theme/mantine/styles'

const loadingRows = Array.from({ length: 10 }, (_, index) => index)

interface StateMessageProps {
  icon: ReactNode
  title: string
  body: string
  action: string
  onAction: (() => void) | undefined
}

function StateMessage({ icon, title, body, action, onAction }: StateMessageProps): ReactElement {
  return (
    <Stack align="center" gap="sm" py="xl">
      <div aria-hidden style={{ color: 'var(--app-color-text-muted)' }}>
        {icon}
      </div>
      <Text fw={600}>{title}</Text>
      <Text size="sm" c="dimmed" ta="center">
        {body}
      </Text>
      {onAction ? (
        <Button variant="light" style={touchStyle} onClick={onAction}>
          {action}
        </Button>
      ) : null}
    </Stack>
  )
}

export function EmptyState({ onCreate }: { onCreate: (() => void) | undefined }): ReactElement {
  const { t } = useTranslation()

  return (
    <StateMessage
      icon={<IconPlus size="var(--app-size-icon-lg)" />}
      title={t('checks.list.empty.title')}
      body={t('checks.list.empty.body')}
      action={t('checks.list.empty.action')}
      onAction={onCreate}
    />
  )
}

export function NoResultsState({ onClearFilters }: { onClearFilters: () => void }): ReactElement {
  const { t } = useTranslation()

  return (
    <StateMessage
      icon={<IconSearch size="var(--app-size-icon-lg)" />}
      title={t('checks.list.noResults.title')}
      body={t('checks.list.noResults.body')}
      action={t('checks.list.noResults.action')}
      onAction={onClearFilters}
    />
  )
}

export function ErrorState({ onRetry }: { onRetry: () => void }): ReactElement {
  const { t } = useTranslation()

  return (
    <StateMessage
      icon={<IconAlertTriangle size="var(--app-size-icon-lg)" />}
      title={t('checks.list.error.title')}
      body={t('checks.list.error.body')}
      action={t('checks.list.error.action')}
      onAction={onRetry}
    />
  )
}

export function MissingSpaceState(): ReactElement {
  const { t } = useTranslation()

  return (
    <PageFrame title={t('checks.list.title')}>
      <Stack align="center" gap="md" py="xl">
        <Text c="dimmed" ta="center">
          {t('checks.list.missingSpace.body')}
        </Text>
        <Button component={Link} to="/" variant="light" style={touchStyle}>
          {t('common.section.backToOverview')}
        </Button>
      </Stack>
    </PageFrame>
  )
}

export function LoadingState(): ReactElement {
  const { t } = useTranslation()

  return (
    <Stack gap="xs" role="status" aria-label={t('checks.list.loading')}>
      {loadingRows.map((row) => (
        <Skeleton key={row} height="var(--app-size-row)" radius="sm" />
      ))}
    </Stack>
  )
}

interface ResultsFooterProps {
  totalCount: number
  hasNextPage: boolean
  onLoadMore: () => void
}

export function ResultsFooter({
  totalCount,
  hasNextPage,
  onLoadMore,
}: ResultsFooterProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Group justify="space-between" wrap="wrap">
      <Text size="sm" c="dimmed">
        {t('checks.list.resultsTotal', { count: totalCount })}
      </Text>
      {hasNextPage ? (
        <Button variant="default" style={touchStyle} onClick={onLoadMore}>
          {t('checks.list.loadMore')}
        </Button>
      ) : null}
    </Group>
  )
}
