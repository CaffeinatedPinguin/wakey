import { Group, Stack, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import type { ChangeEntry, ChangeSource } from './checkDetailsTypes'
import { formatMoment } from './dateFormat'

interface ChangeHistoryTabProps {
  readonly changes: readonly ChangeEntry[]
}

interface ChangeRowProps {
  readonly entry: ChangeEntry
  readonly locale: string
}

const SOURCE_KEYS: Record<ChangeSource, string> = {
  UI: 'checks.details.source.ui',
  API: 'checks.details.source.api',
  FILE: 'checks.details.source.file',
}

function ChangeRow(props: ChangeRowProps): ReactElement {
  const { t } = useTranslation()
  return (
    <Stack gap="xs">
      <Text size="sm" c="dimmed">
        {formatMoment(props.entry.at, props.locale)}
      </Text>
      <Text size="sm" fw={600}>
        {props.entry.summary}
      </Text>
      <Group gap="xs">
        <Text size="sm">{props.entry.actor}</Text>
        <Text size="sm" c="dimmed">
          {t(SOURCE_KEYS[props.entry.source])}
        </Text>
      </Group>
    </Stack>
  )
}

export function ChangeHistoryTab(props: ChangeHistoryTabProps): ReactElement {
  const { t, i18n } = useTranslation()
  if (props.changes.length === 0) {
    return (
      <Text size="sm" c="dimmed">
        {t('checks.details.history.empty')}
      </Text>
    )
  }
  return (
    <Stack gap="md">
      {props.changes.map((entry) => (
        <ChangeRow key={`${entry.at}-${entry.actor}`} entry={entry} locale={i18n.language} />
      ))}
    </Stack>
  )
}
