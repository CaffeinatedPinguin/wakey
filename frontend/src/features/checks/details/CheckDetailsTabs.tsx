import { Tabs, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { useIsMobile } from '../../../shared/ui'
import { CertificateTab } from './CertificateTab'
import { ChangeHistoryTab } from './ChangeHistoryTab'
import { EventsTab } from './EventsTab'
import type { CheckDetailsData, CheckDetailsTab } from './checkDetailsTypes'
import { tabListStyle, touchStyle } from './detailsStyles'

interface TabDefinition {
  readonly key: CheckDetailsTab
  readonly labelKey: string
}

const DESKTOP_TABS: readonly TabDefinition[] = [
  { key: 'events', labelKey: 'checks.details.tabs.events' },
  { key: 'locations', labelKey: 'checks.details.tabs.locations' },
  { key: 'notifications', labelKey: 'checks.details.tabs.notifications' },
  { key: 'configuration', labelKey: 'checks.details.tabs.configuration' },
  { key: 'certificate', labelKey: 'checks.details.tabs.certificate' },
  { key: 'history', labelKey: 'checks.details.tabs.history' },
]

const MOBILE_TABS: readonly TabDefinition[] = [
  { key: 'events', labelKey: 'checks.details.tabs.events' },
  { key: 'locations', labelKey: 'checks.details.tabs.locations' },
  { key: 'history', labelKey: 'checks.details.tabs.history' },
  { key: 'certificate', labelKey: 'checks.details.tabs.certificate' },
]

interface TabContentProps {
  readonly tab: CheckDetailsTab
  readonly data: CheckDetailsData
}

function TabContent(props: TabContentProps): ReactElement {
  const { t } = useTranslation()
  switch (props.tab) {
    case 'events':
      return <EventsTab events={props.data.events} />
    case 'certificate':
      return <CertificateTab certificate={props.data.certificate} />
    case 'history':
      return <ChangeHistoryTab changes={props.data.changes} />
    case 'locations':
    case 'notifications':
    case 'configuration':
      return (
        <Text size="sm" c="dimmed">
          {t('common.section.comingSoon')}
        </Text>
      )
  }
}

interface CheckDetailsTabsProps {
  readonly data: CheckDetailsData
  readonly activeTab: CheckDetailsTab
  readonly onTabChange: (tab: CheckDetailsTab) => void
}

export function CheckDetailsTabs(props: CheckDetailsTabsProps): ReactElement {
  const { t } = useTranslation()
  const isMobile = useIsMobile()
  const tabs = isMobile ? MOBILE_TABS : DESKTOP_TABS

  return (
    <Tabs
      value={props.activeTab}
      onChange={(value) => {
        if (value !== null) {
          props.onTabChange(value as CheckDetailsTab)
        }
      }}
    >
      <Tabs.List style={tabListStyle}>
        {tabs.map((tab) => (
          <Tabs.Tab key={tab.key} value={tab.key} style={touchStyle}>
            {t(tab.labelKey)}
          </Tabs.Tab>
        ))}
      </Tabs.List>
      {tabs.map((tab) => (
        <Tabs.Panel key={tab.key} value={tab.key} pt="md">
          <TabContent tab={tab.key} data={props.data} />
        </Tabs.Panel>
      ))}
    </Tabs>
  )
}
