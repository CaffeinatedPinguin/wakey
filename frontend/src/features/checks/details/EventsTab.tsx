import { Stack, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { StatusBadge } from '../../../shared/ui'
import type { CheckEvent } from './checkDetailsTypes'
import { formatClock } from './dateFormat'

interface EventsTabProps {
  readonly events: readonly CheckEvent[]
}

interface EventRowProps {
  readonly event: CheckEvent
  readonly locale: string
}

function EventRow(props: EventRowProps): ReactElement {
  return (
    <Stack gap="xs">
      <Text size="sm" c="dimmed">
        {formatClock(props.event.at, props.locale)}
      </Text>
      <StatusBadge state={props.event.state} />
      <Text size="sm" fw={600}>
        {props.event.category}
      </Text>
      <Text size="sm">{props.event.summary}</Text>
    </Stack>
  )
}

export function EventsTab(props: EventsTabProps): ReactElement {
  const { t, i18n } = useTranslation()
  if (props.events.length === 0) {
    return (
      <Text size="sm" c="dimmed">
        {t('checks.details.events.empty')}
      </Text>
    )
  }
  return (
    <Stack gap="md">
      {props.events.map((event) => (
        <EventRow key={event.at} event={event} locale={i18n.language} />
      ))}
    </Stack>
  )
}
