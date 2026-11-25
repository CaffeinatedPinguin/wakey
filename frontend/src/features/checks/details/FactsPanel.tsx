import { Paper, Stack, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import type { CheckFacts } from './checkDetailsTypes'
import { DetailRow } from './DetailRow'

interface FactsPanelProps {
  readonly facts: CheckFacts
}

export function FactsPanel(props: FactsPanelProps): ReactElement {
  const { t } = useTranslation()
  return (
    <Paper withBorder p="md" component="aside" aria-label={t('checks.details.facts.title')}>
      <Stack gap="xs">
        <Text size="xs" fw={600} tt="uppercase" c="dimmed">
          {t('checks.details.facts.title')}
        </Text>
        <DetailRow label={t('checks.details.facts.uptime')} value={props.facts.uptime24h} />
        <DetailRow
          label={t('checks.details.facts.dataCoverage')}
          value={props.facts.dataCoverage}
        />
        <DetailRow label={t('checks.details.facts.interval')} value={props.facts.interval} />
        <DetailRow label={t('checks.details.facts.timeout')} value={props.facts.timeout} />
        <DetailRow label={t('checks.details.facts.type')} value={props.facts.type} />
      </Stack>
    </Paper>
  )
}
