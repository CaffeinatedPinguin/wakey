import { Badge } from '@mantine/core'
import {
  IconAlertTriangle,
  IconCircleCheck,
  IconHelpCircle,
  IconOctagon,
  IconTool,
} from '@tabler/icons-react'
import type { ComponentType, ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { StatusState } from './statusState'

interface StatusIconProps {
  size?: number
}

const stateIcons: Record<StatusState, ComponentType<StatusIconProps>> = {
  [StatusState.UP]: IconCircleCheck,
  [StatusState.DEGRADED]: IconAlertTriangle,
  [StatusState.DOWN]: IconOctagon,
  [StatusState.UNKNOWN]: IconHelpCircle,
  [StatusState.MAINTENANCE]: IconTool,
}

interface StatusBadgeProps {
  state: StatusState
}

export function StatusBadge({ state }: StatusBadgeProps): ReactElement {
  const { t } = useTranslation()
  const Icon = stateIcons[state]

  return (
    <Badge color={state} leftSection={<Icon size={14} />}>
      {t(`common.state.${state}`)}
    </Badge>
  )
}
