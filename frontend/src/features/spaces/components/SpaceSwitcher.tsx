import { Button, Drawer, Menu } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { IconChevronDown } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { useCurrentSpace } from '../hooks/useCurrentSpace'
import { useSpaceList } from '../hooks/useSpaceList'
import type { SpaceSummary, SpacesStatus } from '../spaceTypes'
import { CreateSpaceDialog } from './CreateSpaceDialog'
import { DesktopContent, MobileContent } from './SwitcherContent'
import { SwitcherTrigger } from './SwitcherTrigger'
import { currentName as nameFromList, sheetStyles, touchStyle } from './switcherStyles'

export type SpaceSwitcherLayout = 'menu' | 'sheet'

export interface SwitcherListProps {
  spaces: readonly SpaceSummary[]
  currentSlug: string
  currentName?: string | undefined
  status: SpacesStatus
  canCreate: boolean
  hasMore?: boolean
  onLoadMore?: (() => void) | undefined
  onSelect: (slug: string) => void
  onCreate: () => void
  onRetry: () => void
}

export interface SpaceSwitcherProps {
  layout: SpaceSwitcherLayout
  canCreate: boolean
}

export function DesktopSwitcher(props: SwitcherListProps): ReactElement {
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
          <SwitcherTrigger
            status={props.status}
            name={props.currentName ?? nameFromList(props.spaces, props.currentSlug)}
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

export function MobileSwitcher(props: SwitcherListProps): ReactElement {
  const { t } = useTranslation()
  const [opened, { open, close }] = useDisclosure(false)

  return (
    <>
      <Button
        variant="default"
        aria-label={t('spaces.switcher.label')}
        aria-busy={props.status === 'loading' || undefined}
        rightSection={<IconChevronDown size={16} />}
        style={{ ...touchStyle, flex: 1, minWidth: 0 }}
        onClick={open}
      >
        <SwitcherTrigger
          status={props.status}
          name={props.currentName ?? nameFromList(props.spaces, props.currentSlug)}
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

export function SpaceSwitcher({ layout, canCreate }: SpaceSwitcherProps): ReactElement {
  const list = useSpaceList()
  const current = useCurrentSpace()
  const [opened, { open, close }] = useDisclosure(false)
  const currentName = nameFromList(list.spaces, current.slug) ?? current.space?.name
  const listProps: SwitcherListProps = {
    spaces: list.spaces,
    currentSlug: current.slug,
    currentName,
    status: list.status,
    canCreate,
    hasMore: list.hasMore,
    onLoadMore: list.loadMore,
    onSelect: current.select,
    onCreate: open,
    onRetry: list.retry,
  }

  return (
    <>
      {layout === 'menu' ? <DesktopSwitcher {...listProps} /> : <MobileSwitcher {...listProps} />}
      <CreateSpaceDialog opened={opened} onClose={close} onCreated={current.select} />
    </>
  )
}
