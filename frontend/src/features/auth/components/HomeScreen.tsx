import { Badge, Group, Stack, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { PageFrame } from '../../../shared/ui/PageFrame'
import type { Me } from '../api/auth.schemas'

interface HomeScreenProps {
  me: Me
}

export function HomeScreen({ me }: HomeScreenProps): ReactElement {
  const { t } = useTranslation()

  return (
    <PageFrame title={t('common.nav.overview')}>
      <Stack gap="sm">
        <Text>{t('auth.home.signedInAs')}</Text>
        <Group gap="xs">
          <Text>{me.subject}</Text>
          {me.admin ? <Badge>{t('auth.home.admin')}</Badge> : null}
        </Group>
      </Stack>
    </PageFrame>
  )
}
