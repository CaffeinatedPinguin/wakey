import { Group, Stack, Text, Title } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { StatusBadge } from '../../../shared/ui'
import { formatRelativeAge } from './dateFormat'
import type { CheckAction, CheckDetailsData } from './checkDetailsTypes'
import { targetStyle } from './detailsStyles'
import { CheckActionBar } from './CheckActionBar'

interface CheckHeaderProps {
  readonly data: CheckDetailsData
  readonly now: Date
  readonly onAction: (action: CheckAction) => void
  readonly unavailableActions?: readonly CheckAction[]
}

export function CheckHeader(props: CheckHeaderProps): ReactElement {
  const { t, i18n } = useTranslation()
  const unknownAge =
    props.data.state === 'unknown'
      ? formatRelativeAge(props.data.lastCheckedAt, props.now, i18n.language)
      : null

  return (
    <Stack gap="sm">
      <Group justify="space-between" align="flex-start" gap="md" wrap="wrap">
        <Group gap="sm" align="center" wrap="wrap">
          <Title order={1}>{props.data.name}</Title>
          <StatusBadge state={props.data.state} />
          <Text size="sm" c="dimmed" style={targetStyle}>
            {props.data.target}
          </Text>
        </Group>
        <CheckActionBar
          paused={props.data.paused}
          onAction={props.onAction}
          {...(props.unavailableActions === undefined
            ? {}
            : { unavailableActions: props.unavailableActions })}
        />
      </Group>
      {unknownAge === null ? null : (
        <Text size="sm" c="dimmed">
          {t('checks.details.unknown.age', { age: unknownAge })}
        </Text>
      )}
    </Stack>
  )
}
