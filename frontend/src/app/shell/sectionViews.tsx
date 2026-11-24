import type { ReactElement } from 'react'
import { EmptySectionView } from './EmptySectionView'
import {
  checksSection,
  incidentsSection,
  settingsSection,
  statusPagesSection,
} from './panelNavigation'

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
  return <EmptySectionView section={settingsSection} />
}
