import { Button, Group, Menu, Modal, Stack, Text } from '@mantine/core'
import { IconDots, IconPlayerPause, IconPlayerPlay, IconTrash } from '@tabler/icons-react'
import type { ComponentType, ReactElement } from 'react'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { BulkCheckAction } from '../../../shared/api/generated/enums'
import { useIsMobile } from '../../../shared/ui'
import { touchStyle } from './detailsStyles'

interface CheckActionBarProps {
  readonly paused: boolean
  readonly busy: boolean
  readonly onAction: (action: BulkCheckAction) => void
}

interface ActionListProps {
  readonly paused: boolean
  readonly busy: boolean
  readonly onAction: (action: BulkCheckAction) => void
  readonly onDelete: () => void
}

interface DeleteCheckDialogProps {
  readonly opened: boolean
  readonly onClose: () => void
  readonly onConfirm: () => void
}

type IconComponent = ComponentType<{ size?: number }>

const ACTION_LABEL_KEYS: Record<BulkCheckAction, string> = {
  [BulkCheckAction.PAUSE]: 'checks.details.actions.pause',
  [BulkCheckAction.RESUME]: 'checks.details.actions.resume',
  [BulkCheckAction.DELETE]: 'checks.details.actions.delete',
}

const ACTION_ICONS: Record<BulkCheckAction, IconComponent> = {
  [BulkCheckAction.PAUSE]: IconPlayerPause,
  [BulkCheckAction.RESUME]: IconPlayerPlay,
  [BulkCheckAction.DELETE]: IconTrash,
}

function resolvePauseAction(paused: boolean): BulkCheckAction {
  return paused ? BulkCheckAction.RESUME : BulkCheckAction.PAUSE
}

function DesktopActions(props: ActionListProps): ReactElement {
  const { t } = useTranslation()
  const pauseAction = resolvePauseAction(props.paused)

  return (
    <Group gap="xs">
      <Button
        variant="default"
        disabled={props.busy}
        onClick={() => {
          props.onAction(pauseAction)
        }}
      >
        {t(ACTION_LABEL_KEYS[pauseAction])}
      </Button>
      <Button variant="default" color="danger" disabled={props.busy} onClick={props.onDelete}>
        {t(ACTION_LABEL_KEYS[BulkCheckAction.DELETE])}
      </Button>
    </Group>
  )
}

interface MobileMenuActionProps {
  readonly action: BulkCheckAction
  readonly busy: boolean
  readonly onAction: (action: BulkCheckAction) => void
}

function MobileMenuAction({ action, busy, onAction }: MobileMenuActionProps): ReactElement {
  const { t } = useTranslation()
  const Icon = ACTION_ICONS[action]

  return (
    <Menu.Item
      leftSection={<Icon size={16} />}
      style={touchStyle}
      disabled={busy}
      onClick={() => {
        onAction(action)
      }}
    >
      {t(ACTION_LABEL_KEYS[action])}
    </Menu.Item>
  )
}

function MobileActions(props: ActionListProps): ReactElement {
  const { t } = useTranslation()
  const pauseAction = resolvePauseAction(props.paused)

  return (
    <Menu position="bottom-end" width="var(--app-size-dropdown-min)">
      <Menu.Target>
        <Button
          variant="default"
          leftSection={<IconDots size={16} />}
          aria-label={t('checks.details.actions.menu')}
          style={touchStyle}
          disabled={props.busy}
        >
          {t('checks.details.actions.menu')}
        </Button>
      </Menu.Target>
      <Menu.Dropdown>
        <MobileMenuAction action={pauseAction} busy={props.busy} onAction={props.onAction} />
        <Menu.Item
          leftSection={<IconTrash size={16} />}
          style={touchStyle}
          disabled={props.busy}
          onClick={props.onDelete}
        >
          {t(ACTION_LABEL_KEYS[BulkCheckAction.DELETE])}
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  )
}

function DeleteCheckDialog(props: DeleteCheckDialogProps): ReactElement {
  const { t } = useTranslation()
  const confirmed = useRef(false)

  useEffect(() => {
    if (props.opened) {
      confirmed.current = false
    }
  }, [props.opened])

  function confirm(): void {
    if (confirmed.current) {
      return
    }
    confirmed.current = true
    props.onConfirm()
  }

  return (
    <Modal opened={props.opened} onClose={props.onClose} title={t('checks.details.delete.title')}>
      <Stack gap="md">
        <Text size="sm">{t('checks.details.delete.body')}</Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={props.onClose}>
            {t('checks.details.delete.cancel')}
          </Button>
          <Button color="danger" onClick={confirm}>
            {t('checks.details.delete.confirm')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}

export function CheckActionBar(props: CheckActionBarProps): ReactElement {
  const isMobile = useIsMobile()
  const [confirmOpen, setConfirmOpen] = useState(false)

  function requestDelete(): void {
    setConfirmOpen(true)
  }

  function confirmDelete(): void {
    setConfirmOpen(false)
    props.onAction(BulkCheckAction.DELETE)
  }

  const actions = {
    paused: props.paused,
    busy: props.busy,
    onAction: props.onAction,
    onDelete: requestDelete,
  }
  return (
    <>
      {isMobile ? <MobileActions {...actions} /> : <DesktopActions {...actions} />}
      <DeleteCheckDialog
        opened={confirmOpen}
        onClose={() => {
          setConfirmOpen(false)
        }}
        onConfirm={confirmDelete}
      />
    </>
  )
}
