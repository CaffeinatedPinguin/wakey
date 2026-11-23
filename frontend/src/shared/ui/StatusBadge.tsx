import { Badge } from '@mantine/core'
import {
  IconAlertTriangle,
  IconCircleCheck,
  IconHelpCircle,
  IconKey,
  IconOctagon,
} from '@tabler/icons-react'
import type { ComponentType, ReactElement } from 'react'
import { useTranslation } from 'react-i18next'

export type StatusState = 'up' | 'degraded' | 'down' | 'unknown' | 'maintenance'

interface StatusIconProps {
  size?: number
}

const stateIcons: Record<StatusState, ComponentType<StatusIconProps>> = {
  up: IconCircleCheck,
  degraded: IconAlertTriangle,
  down: IconOctagon,
  unknown: IconHelpCircle,
  maintenance: IconKey,
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
