import { Button, Stack } from '@mantine/core'
import { Link, useSearch } from '@tanstack/react-router'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { SettingsPage } from '../../features/settings'
import { spaceFromSearch } from '../../features/spaces'
import { PageFrame } from '../../shared/ui/PageFrame'
import { EmptySectionView } from './EmptySectionView'
import { checksSection, incidentsSection, statusPagesSection } from './panelNavigation'

export function ChecksSectionView(): ReactElement {
  return <EmptySectionView section={checksSection} />
}

export function IncidentsSectionView(): ReactElement {
  return <EmptySectionView section={incidentsSection} />
}

export function StatusPagesSectionView(): ReactElement {
  return <EmptySectionView section={statusPagesSection} />
}

export function SettingsSectionView(): ReactElement {
  const { t } = useTranslation()
  const search = useSearch({ strict: false })
  const spaceSlug = spaceFromSearch(search)

  return (
    <PageFrame title={t('common.nav.settings')}>
      <Stack gap="md">
        <Button component={Link} to="/" variant="subtle">
          {t('common.section.backToOverview')}
        </Button>
        <SettingsPage spaceSlug={spaceSlug} />
      </Stack>
    </PageFrame>
  )
}
