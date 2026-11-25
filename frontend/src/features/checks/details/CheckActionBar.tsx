import { Button, Group, Menu, Modal, Stack, Text } from '@mantine/core'
import {
  IconCopy,
  IconDots,
  IconEdit,
  IconPlayerPause,
  IconPlayerPlay,
  IconRefresh,
  IconTrash,
} from '@tabler/icons-react'
import type { ComponentType, ReactElement } from 'react'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { CheckAction } from './checkDetailsTypes'
import { touchStyle } from './detailsStyles'
import { useIsMobile } from './useIsMobile'

interface CheckActionBarProps {
  readonly paused: boolean
  readonly onAction: (action: CheckAction) => void
}

interface ActionListProps {
  readonly paused: boolean
  readonly onAction: (action: CheckAction) => void
  readonly onDelete: () => void
}

interface DeleteCheckDialogProps {
  readonly opened: boolean
  readonly onClose: () => void
  readonly onConfirm: () => void
}

interface MobileMenuItemProps {
  readonly action: CheckAction
  readonly paused: boolean
  readonly onAction: (action: CheckAction) => void
  readonly onDelete: () => void
}

type IconComponent = ComponentType<{ size?: number }>

const ACTION_LABEL_KEYS: Record<CheckAction, string> = {
  'check-now': 'checks.details.actions.checkNow',
  pause: 'checks.details.actions.pause',
  resume: 'checks.details.actions.resume',
  edit: 'checks.details.actions.edit',
  duplicate: 'checks.details.actions.duplicate',
  delete: 'checks.details.actions.delete',
}

const ACTION_ICONS: Record<CheckAction, IconComponent> = {
  'check-now': IconRefresh,
  pause: IconPlayerPause,
  resume: IconPlayerPlay,
  edit: IconEdit,
  duplicate: IconCopy,
  delete: IconTrash,
}

const DESKTOP_ACTIONS: readonly CheckAction[] = ['check-now', 'pause', 'edit', 'duplicate']
const MOBILE_ACTIONS: readonly CheckAction[] = ['check-now', 'pause', 'edit', 'duplicate', 'delete']

function resolveAction(action: CheckAction, paused: boolean): CheckAction {
  return action === 'pause' && paused ? 'resume' : action
}

function DesktopActions(props: ActionListProps): ReactElement {
  const { t } = useTranslation()
  return (
    <Group gap="xs">
      {DESKTOP_ACTIONS.map((action) => {
        const resolved = resolveAction(action, props.paused)
        return (
          <Button
            key={action}
            variant={action === 'check-now' ? 'filled' : 'default'}
            onClick={() => {
              props.onAction(resolved)
            }}
          >
            {t(ACTION_LABEL_KEYS[resolved])}
          </Button>
        )
      })}
      <Button variant="default" color="danger" onClick={props.onDelete}>
        {t(ACTION_LABEL_KEYS.delete)}
      </Button>
    </Group>
  )
}

function MobileMenuItem(props: MobileMenuItemProps): ReactElement {
  const { t } = useTranslation()
  const resolved = resolveAction(props.action, props.paused)
  const Icon = ACTION_ICONS[resolved]

  function handleClick(): void {
    if (resolved === 'delete') {
      props.onDelete()
      return
    }
    props.onAction(resolved)
  }

  return (
    <Menu.Item leftSection={<Icon size={16} />} style={touchStyle} onClick={handleClick}>
      {t(ACTION_LABEL_KEYS[resolved])}
    </Menu.Item>
  )
}

function MobileActions(props: ActionListProps): ReactElement {
  const { t } = useTranslation()
  return (
    <Menu position="bottom-end" width="var(--app-size-dropdown-min)">
      <Menu.Target>
        <Button
          variant="default"
          leftSection={<IconDots size={16} />}
          aria-label={t('checks.details.actions.menu')}
          style={touchStyle}
        >
          {t('checks.details.actions.menu')}
        </Button>
      </Menu.Target>
      <Menu.Dropdown>
        {MOBILE_ACTIONS.map((action) => (
          <MobileMenuItem
            key={action}
            action={action}
            paused={props.paused}
            onAction={props.onAction}
            onDelete={props.onDelete}
          />
        ))}
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
    props.onAction('delete')
  }

  const actions = { paused: props.paused, onAction: props.onAction, onDelete: requestDelete }
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
