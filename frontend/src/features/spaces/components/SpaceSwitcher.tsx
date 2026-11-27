import { Button, Drawer, Menu } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { IconChevronDown } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { useCurrentSpace } from '../hooks/useCurrentSpace'
import { useSpaceList } from '../hooks/useSpaceList'
import { SpaceSwitcherLayout, SpacesStatus } from '../spaceTypes'
import type { SpaceListActions, SpaceListState } from '../spaceTypes'
import { CreateSpaceDialog } from './CreateSpaceDialog'
import { DesktopContent, MobileContent } from './SwitcherContent'
import { SwitcherTrigger } from './SwitcherTrigger'
import { currentName as nameFromList, sheetStyles, touchStyle } from './switcherStyles'

const SWITCHER_ICON_SIZE = 16

export interface SwitcherListProps {
  list: SpaceListState
  actions: SpaceListActions
}

export interface SpaceSwitcherProps {
  layout: SpaceSwitcherLayout
  canCreate: boolean
}

export function DesktopSwitcher({ list, actions }: SwitcherListProps): ReactElement {
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
          aria-busy={list.status === SpacesStatus.LOADING || undefined}
          rightSection={<IconChevronDown size={SWITCHER_ICON_SIZE} />}
        >
          <SwitcherTrigger
            status={list.status}
            name={list.currentName ?? nameFromList(list.spaces, list.currentSlug)}
            compact={false}
          />
        </Button>
      </Menu.Target>
      <Menu.Dropdown>
        <DesktopContent list={list} actions={actions} close={close} />
      </Menu.Dropdown>
    </Menu>
  )
}

export function MobileSwitcher({ list, actions }: SwitcherListProps): ReactElement {
  const { t } = useTranslation()
  const [opened, { open, close }] = useDisclosure(false)

  return (
    <>
      <Button
        variant="default"
        aria-label={t('spaces.switcher.label')}
        aria-busy={list.status === SpacesStatus.LOADING || undefined}
        rightSection={<IconChevronDown size={SWITCHER_ICON_SIZE} />}
        style={{ ...touchStyle, flex: 1, minWidth: 0 }}
        onClick={open}
      >
        <SwitcherTrigger
          status={list.status}
          name={list.currentName ?? nameFromList(list.spaces, list.currentSlug)}
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
        <MobileContent list={list} actions={actions} close={close} />
      </Drawer>
    </>
  )
}

export function SpaceSwitcher({ layout, canCreate }: SpaceSwitcherProps): ReactElement {
  const list = useSpaceList()
  const current = useCurrentSpace()
  const [opened, { open, close }] = useDisclosure(false)
  const currentName = nameFromList(list.spaces, current.slug) ?? current.space?.name
  const listState: SpaceListState = {
    spaces: list.spaces,
    currentSlug: current.slug,
    currentName,
    status: list.status,
    canCreate,
    hasMore: list.hasMore,
  }
  const actions: SpaceListActions = {
    onLoadMore: list.loadMore,
    onSelect: current.select,
    onCreate: open,
    onRetry: list.retry,
  }

  return (
    <>
      {layout === SpaceSwitcherLayout.MENU ? (
        <DesktopSwitcher list={listState} actions={actions} />
      ) : (
        <MobileSwitcher list={listState} actions={actions} />
      )}
      <CreateSpaceDialog opened={opened} onClose={close} onCreated={current.select} />
    </>
  )
}
