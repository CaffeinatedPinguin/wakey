import type { QueryStatus } from '@tanstack/react-query'
import type { BulkCheckAction } from '../../../shared/api/generated/enums'
import type { StatusState } from '../../../shared/ui'

export interface CheckFacts {
  readonly interval: string
  readonly timeout: string
  readonly type: string
}

export interface CheckDetailsData {
  readonly name: string
  readonly target: string
  readonly state: StatusState
  readonly paused: boolean
  readonly lastCheckedAt: string
  readonly facts: CheckFacts
}

export interface CheckDetailsViewProps {
  readonly data: CheckDetailsData | null
  readonly status: QueryStatus
  readonly notFound: boolean
  readonly onAction: (action: BulkCheckAction) => void
  readonly onRetry: () => void
  /** Current time injected by the caller so relative ages stay deterministic. */
  readonly now: Date
  /** True while a header action is in flight, so its buttons stay disabled. */
  readonly busy: boolean
}
