import { Box, Button, Menu, Skeleton, Stack, Text, UnstyledButton } from '@mantine/core'
import { IconCheck, IconPlus, IconRefresh } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { SpacesStatus } from '../spaceTypes'
import type { SpaceListActions, SpaceListState, SpaceSummary } from '../spaceTypes'
import { avatarStyle, sheetRowStyle, touchStyle } from './switcherStyles'

const SMALL_ICON_SIZE = 16
const LARGE_ICON_SIZE = 18
const LABEL_FONT_WEIGHT = 500

export interface SpaceListContentProps {
  list: SpaceListState
  actions: SpaceListActions
  close: () => void
}

export function LoadingContent(): ReactElement {
  const { t } = useTranslation()

  return (
    <Stack gap="xs" role="status" aria-label={t('spaces.switcher.loading')}>
      <Skeleton height="var(--app-type-metadata-size)" />
      <Skeleton height="var(--app-type-metadata-size)" />
      <Skeleton height="var(--app-type-metadata-size)" />
    </Stack>
  )
}

export function ErrorContent({ onRetry }: { onRetry: () => void }): ReactElement {
  const { t } = useTranslation()

  return (
    <Stack gap="sm" align="flex-start">
      <Text size="sm">{t('spaces.switcher.error')}</Text>
      <Button
        variant="light"
        leftSection={<IconRefresh size={SMALL_ICON_SIZE} />}
        onClick={onRetry}
        style={touchStyle}
      >
        {t('spaces.switcher.retry')}
      </Button>
    </Stack>
  )
}

export function EmptyContent({
  canCreate,
  onCreate,
}: {
  canCreate: boolean
  onCreate: () => void
}): ReactElement {
  const { t } = useTranslation()

  return (
    <Stack gap="sm" align="flex-start">
      <Text size="sm">
        {t(canCreate ? 'spaces.switcher.emptyAdmin' : 'spaces.switcher.emptyMember')}
      </Text>
      {canCreate ? (
        <Button
          variant="light"
          leftSection={<IconPlus size={SMALL_ICON_SIZE} />}
          onClick={onCreate}
          style={touchStyle}
        >
          {t('spaces.switcher.create')}
        </Button>
      ) : null}
    </Stack>
  )
}

function LoadMoreButton({ onLoadMore }: { onLoadMore: () => void }): ReactElement {
  const { t } = useTranslation()

  return (
    <Button variant="light" onClick={onLoadMore} style={touchStyle}>
      {t('spaces.switcher.loadMore')}
    </Button>
  )
}

function DesktopListItem({
  space,
  currentSlug,
  onSelect,
  close,
}: {
  space: SpaceSummary
  currentSlug: string
  onSelect: (slug: string) => void
  close: () => void
}): ReactElement {
  const isCurrent = space.slug === currentSlug

  return (
    <Menu.Item
      aria-label={space.name}
      aria-current={isCurrent ? 'true' : undefined}
      leftSection={isCurrent ? <IconCheck size={SMALL_ICON_SIZE} /> : undefined}
      onClick={() => {
        onSelect(space.slug)
        close()
      }}
    >
      {space.name}
    </Menu.Item>
  )
}

interface DesktopFooterProps {
  hasMore: boolean
  onLoadMore?: (() => void) | undefined
  canCreate: boolean
  onCreate: () => void
}

function DesktopFooter({
  hasMore,
  onLoadMore,
  canCreate,
  onCreate,
}: DesktopFooterProps): ReactElement {
  const { t } = useTranslation()

  return (
    <>
      {hasMore && onLoadMore !== undefined ? <LoadMoreButton onLoadMore={onLoadMore} /> : null}
      {canCreate ? (
        <>
          <Menu.Divider />
          <Menu.Item leftSection={<IconPlus size={SMALL_ICON_SIZE} />} onClick={onCreate}>
            {t('spaces.switcher.create')}
          </Menu.Item>
        </>
      ) : null}
    </>
  )
}

export function DesktopContent({ list, actions, close }: SpaceListContentProps): ReactElement {
  if (list.status === SpacesStatus.LOADING) {
    return <LoadingContent />
  }
  if (list.status === SpacesStatus.ERROR) {
    return <ErrorContent onRetry={actions.onRetry} />
  }
  if (list.status === SpacesStatus.EMPTY) {
    return <EmptyContent canCreate={list.canCreate} onCreate={actions.onCreate} />
  }

  return (
    <>
      {list.spaces.map((space) => (
        <DesktopListItem
          key={space.slug}
          space={space}
          currentSlug={list.currentSlug}
          onSelect={actions.onSelect}
          close={close}
        />
      ))}
      <DesktopFooter
        hasMore={list.hasMore === true}
        onLoadMore={actions.onLoadMore}
        canCreate={list.canCreate}
        onCreate={actions.onCreate}
      />
    </>
  )
}

function SheetRow({
  space,
  currentSlug,
  onSelect,
  close,
}: {
  space: SpaceSummary
  currentSlug: string
  onSelect: (slug: string) => void
  close: () => void
}): ReactElement {
  const isCurrent = space.slug === currentSlug

  return (
    <UnstyledButton
      aria-label={space.name}
      aria-current={isCurrent ? 'true' : undefined}
      style={sheetRowStyle}
      onClick={() => {
        onSelect(space.slug)
        close()
      }}
    >
      <Box component="span" style={avatarStyle} aria-hidden>
        {space.name.slice(0, 1).toUpperCase()}
      </Box>
      <Text size="sm" fw={LABEL_FONT_WEIGHT}>
        {space.name}
      </Text>
      {isCurrent ? (
        <IconCheck size={LARGE_ICON_SIZE} style={{ marginInlineStart: 'auto' }} />
      ) : null}
    </UnstyledButton>
  )
}

function MobileCreateRow({
  onCreate,
  close,
}: {
  onCreate: () => void
  close: () => void
}): ReactElement {
  const { t } = useTranslation()

  return (
    <UnstyledButton
      style={sheetRowStyle}
      onClick={() => {
        onCreate()
        close()
      }}
    >
      <IconPlus size={LARGE_ICON_SIZE} />
      <Text size="sm" fw={LABEL_FONT_WEIGHT}>
        {t('spaces.switcher.create')}
      </Text>
    </UnstyledButton>
  )
}

interface MobileFooterProps {
  hasMore: boolean
  onLoadMore?: (() => void) | undefined
  canCreate: boolean
  onCreate: () => void
  close: () => void
}

function MobileFooter({
  hasMore,
  onLoadMore,
  canCreate,
  onCreate,
  close,
}: MobileFooterProps): ReactElement {
  return (
    <>
      {hasMore && onLoadMore !== undefined ? <LoadMoreButton onLoadMore={onLoadMore} /> : null}
      {canCreate ? <MobileCreateRow onCreate={onCreate} close={close} /> : null}
    </>
  )
}

export function MobileContent({ list, actions, close }: SpaceListContentProps): ReactElement {
  if (list.status === SpacesStatus.LOADING) {
    return <LoadingContent />
  }
  if (list.status === SpacesStatus.ERROR) {
    return <ErrorContent onRetry={actions.onRetry} />
  }
  if (list.status === SpacesStatus.EMPTY) {
    return <EmptyContent canCreate={list.canCreate} onCreate={actions.onCreate} />
  }

  return (
    <Stack gap={0}>
      {list.spaces.map((space) => (
        <SheetRow
          key={space.slug}
          space={space}
          currentSlug={list.currentSlug}
          onSelect={actions.onSelect}
          close={close}
        />
      ))}
      <MobileFooter
        hasMore={list.hasMore === true}
        onLoadMore={actions.onLoadMore}
        canCreate={list.canCreate}
        onCreate={actions.onCreate}
        close={close}
      />
    </Stack>
  )
}
