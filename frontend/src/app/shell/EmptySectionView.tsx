import { Button, Stack, Text } from '@mantine/core'
import { Link } from '@tanstack/react-router'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { PageFrame } from '../../shared/ui/PageFrame'
import type { PanelSection } from './panelNavigation'

interface EmptySectionViewProps {
  section: PanelSection
}

const iconStyle = {
  color: 'var(--app-color-text-muted)',
}

export function EmptySectionView({ section }: EmptySectionViewProps): ReactElement {
  const { t } = useTranslation()
  const { labelKey, Icon } = section

  return (
    <PageFrame title={t(labelKey)}>
      <Stack align="center" gap="md" py="xl">
        <Icon size="var(--app-size-control-large)" style={iconStyle} aria-hidden />
        <Text c="dimmed" ta="center">
          {t('common.section.comingSoon')}
        </Text>
        <Button component={Link} to="/" variant="light">
          {t('common.section.backToOverview')}
        </Button>
      </Stack>
    </PageFrame>
  )
}
