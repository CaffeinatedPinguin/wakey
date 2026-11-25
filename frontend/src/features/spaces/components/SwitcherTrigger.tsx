import { Skeleton, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { SpacesStatus } from '../spaceTypes'

interface SwitcherTriggerProps {
  status: SpacesStatus
  name: string | undefined
  compact: boolean
}

export function SwitcherTrigger({ status, name, compact }: SwitcherTriggerProps): ReactElement {
  const { t } = useTranslation()
  if (status === SpacesStatus.LOADING) {
    return (
      <Skeleton height="var(--app-type-metadata-line)" width="var(--app-space-96)" radius="xl" />
    )
  }
  if (status === SpacesStatus.EMPTY) {
    return <Text size="sm">{t('spaces.switcher.emptyLabel')}</Text>
  }
  if (status === SpacesStatus.ERROR) {
    return <Text size="sm">{t('spaces.switcher.errorLabel')}</Text>
  }
  if (compact || name === undefined) {
    return (
      <Text size="sm" truncate>
        {name ?? t('spaces.switcher.emptyLabel')}
      </Text>
    )
  }
  return <Text size="sm">{t('spaces.switcher.current', { name })}</Text>
}
