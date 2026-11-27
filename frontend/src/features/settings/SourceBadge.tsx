import { Badge, Box, Group, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { SettingSource } from '../../shared/api/generated/enums'
import { ownSourceStyle, sourceDotStyle } from './styles'

interface SourceBadgeProps {
  source: SettingSource
  overriddenValue: string | undefined
}

function OwnSource({ overriddenValue }: { overriddenValue: string | undefined }): ReactElement {
  const { t } = useTranslation()
  const text =
    overriddenValue === undefined
      ? t('settings.source.space')
      : t('settings.source.overrides', { value: overriddenValue })

  return (
    <Group gap="xs" wrap="nowrap">
      <Box aria-hidden w="var(--app-space-8)" h="var(--app-space-8)" style={sourceDotStyle} />
      <Text size="xs" fw={500} style={ownSourceStyle}>
        {text}
      </Text>
    </Group>
  )
}

export function SourceBadge({ source, overriddenValue }: SourceBadgeProps): ReactElement {
  const { t } = useTranslation()
  const sourceKey = source.toLowerCase()

  if (source === SettingSource.SPACE) {
    return <OwnSource overriddenValue={overriddenValue} />
  }

  return (
    <Group gap="xs" wrap="nowrap">
      <Badge variant="default" size="sm">
        {t(`settings.source.${sourceKey}`)}
      </Badge>
      <Text size="xs" c="dimmed">
        {t(`settings.source.${sourceKey}Hint`)}
      </Text>
    </Group>
  )
}
