import { Button, Drawer, Menu, Skeleton, Text } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { IconChevronDown } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { DesktopContent, MobileContent } from './SwitcherContent'
import type { SpaceSummary, SpacesStatus } from '../spaceTypes'
import { currentName, sheetStyles, touchStyle } from './switcherStyles'

export type SpaceSwitcherLayout = 'menu' | 'sheet'

export interface SpaceSwitcherProps {
  spaces: readonly SpaceSummary[]
  currentSlug: string
  status: SpacesStatus
  canCreate: boolean
  hasMore?: boolean
  onLoadMore?: (() => void) | undefined
  layout: SpaceSwitcherLayout
  onSelect: (slug: string) => void
  onCreate: () => void
  onRetry: () => void
}

export type SpaceSwitcherDataProps = Omit<SpaceSwitcherProps, 'layout'>

interface TriggerTextProps {
  status: SpacesStatus
  name: string | undefined
  compact: boolean
}

function TriggerText({ status, name, compact }: TriggerTextProps): ReactElement {
  const { t } = useTranslation()
  if (status === 'loading') {
    return (
      <Skeleton height="var(--app-type-metadata-line)" width="var(--app-space-96)" radius="xl" />
    )
  }
  if (status === 'empty') {
    return <Text size="sm">{t('spaces.switcher.emptyLabel')}</Text>
  }
  if (status === 'error') {
    return <Text size="sm">{t('spaces.switcher.errorLabel')}</Text>
  }
  if (compact || name === undefined) {
    return <Text size="sm">{name ?? t('spaces.switcher.emptyLabel')}</Text>
  }
  return <Text size="sm">{t('spaces.switcher.current', { name })}</Text>
}

export function DesktopSwitcher(props: SpaceSwitcherProps): ReactElement {
  const { t } = useTranslation()
  const [opened, { open, close }] = useDisclosure(false)

  return (
    <Menu
      opened={opened}
      onClose={close}
      onChange={(value) => {
        if (value) {
          open()
        } else {
          close()
        }
      }}
      width="var(--app-size-dropdown-min)"
    >
      <Menu.Target>
        <Button
          variant="default"
          aria-label={t('spaces.switcher.label')}
          aria-busy={props.status === 'loading' || undefined}
          rightSection={<IconChevronDown size={16} />}
        >
          <TriggerText
            status={props.status}
            name={currentName(props.spaces, props.currentSlug)}
            compact={false}
          />
        </Button>
      </Menu.Target>
      <Menu.Dropdown>
        <DesktopContent {...props} close={close} />
      </Menu.Dropdown>
    </Menu>
  )
}

export function MobileSwitcher(props: SpaceSwitcherProps): ReactElement {
  const { t } = useTranslation()
  const [opened, { open, close }] = useDisclosure(false)

  return (
    <>
      <Button
        variant="default"
        aria-label={t('spaces.switcher.label')}
        aria-busy={props.status === 'loading' || undefined}
        rightSection={<IconChevronDown size={16} />}
        style={touchStyle}
        onClick={open}
      >
        <TriggerText
          status={props.status}
          name={currentName(props.spaces, props.currentSlug)}
          compact
        />
      </Button>
      <Drawer
        opened={opened}
        onClose={close}
        position="bottom"
        size="auto"
        title={t('spaces.switcher.sheetTitle')}
        styles={sheetStyles}
      >
        <MobileContent {...props} close={close} />
      </Drawer>
    </>
  )
}

export function SpaceSwitcher(props: SpaceSwitcherProps): ReactElement {
  return props.layout === 'menu' ? <DesktopSwitcher {...props} /> : <MobileSwitcher {...props} />
}
