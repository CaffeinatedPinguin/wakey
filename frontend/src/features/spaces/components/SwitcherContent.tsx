import { Box, Button, Menu, Skeleton, Stack, Text, UnstyledButton } from '@mantine/core'
import { IconCheck, IconPlus, IconRefresh } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import type { SpaceSummary, SpacesStatus } from '../spaceTypes'
import { avatarStyle, sheetRowStyle, touchStyle } from './switcherStyles'

interface ListProps {
  spaces: readonly SpaceSummary[]
  currentSlug: string
  status: SpacesStatus
  canCreate: boolean
  onSelect: (slug: string) => void
  onCreate: () => void
  onRetry: () => void
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
        leftSection={<IconRefresh size={16} />}
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
          leftSection={<IconPlus size={16} />}
          onClick={onCreate}
          style={touchStyle}
        >
          {t('spaces.switcher.create')}
        </Button>
      ) : null}
    </Stack>
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
      leftSection={isCurrent ? <IconCheck size={16} /> : undefined}
      onClick={() => {
        onSelect(space.slug)
        close()
      }}
    >
      {space.name}
    </Menu.Item>
  )
}

export function DesktopContent({
  currentSlug,
  close,
  ...list
}: ListProps & { close: () => void }): ReactElement {
  const { t } = useTranslation()
  if (list.status === 'loading') {
    return <LoadingContent />
  }
  if (list.status === 'error') {
    return <ErrorContent onRetry={list.onRetry} />
  }
  if (list.status === 'empty') {
    return <EmptyContent canCreate={list.canCreate} onCreate={list.onCreate} />
  }

  return (
    <>
      {list.spaces.map((space) => (
        <DesktopListItem
          key={space.slug}
          space={space}
          currentSlug={currentSlug}
          onSelect={list.onSelect}
          close={close}
        />
      ))}
      {list.canCreate ? (
        <>
          <Menu.Divider />
          <Menu.Item leftSection={<IconPlus size={16} />} onClick={list.onCreate}>
            {t('spaces.switcher.create')}
          </Menu.Item>
        </>
      ) : null}
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
      <Text size="sm" fw={500}>
        {space.name}
      </Text>
      {isCurrent ? <IconCheck size={18} style={{ marginInlineStart: 'auto' }} /> : null}
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
      <IconPlus size={18} />
      <Text size="sm" fw={500}>
        {t('spaces.switcher.create')}
      </Text>
    </UnstyledButton>
  )
}

export function MobileContent({
  currentSlug,
  close,
  ...list
}: ListProps & { close: () => void }): ReactElement {
  if (list.status === 'loading') {
    return <LoadingContent />
  }
  if (list.status === 'error') {
    return <ErrorContent onRetry={list.onRetry} />
  }
  if (list.status === 'empty') {
    return <EmptyContent canCreate={list.canCreate} onCreate={list.onCreate} />
  }

  return (
    <Stack gap={0}>
      {list.spaces.map((space) => (
        <SheetRow
          key={space.slug}
          space={space}
          currentSlug={currentSlug}
          onSelect={list.onSelect}
          close={close}
        />
      ))}
      {list.canCreate ? <MobileCreateRow onCreate={list.onCreate} close={close} /> : null}
    </Stack>
  )
}
