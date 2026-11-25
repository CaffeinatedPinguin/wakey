import type { StatusState } from '../../../shared/ui'

/** Actions emitted by the check details header. */
export type CheckAction = 'check-now' | 'pause' | 'resume' | 'edit' | 'duplicate' | 'delete'

/** Tabs of the check details view. */
export type CheckDetailsTab =
  'events' | 'locations' | 'notifications' | 'configuration' | 'certificate' | 'history'

/** How a configuration change reached the server. */
export type ChangeSource = 'UI' | 'API' | 'FILE'

export type CheckDetailsStatus = 'loading' | 'ready' | 'notFound' | 'error'

export interface CheckEvent {
  readonly at: string
  readonly state: StatusState
  readonly category: string
  readonly summary: string
}

export interface CertificateInfo {
  readonly issuer: string
  readonly validTo: string
  readonly daysLeft: number
  readonly fingerprint: string
}

export interface ChangeEntry {
  readonly at: string
  readonly actor: string
  readonly source: ChangeSource
  readonly summary: string
}

export interface CheckFacts {
  readonly uptime24h: string
  readonly dataCoverage: string
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
  /** Reasons for the "configuration needs attention" hint; empty hides the banner. */
  readonly configIssues: readonly string[]
  readonly events: readonly CheckEvent[]
  readonly certificate: CertificateInfo | null
  readonly changes: readonly ChangeEntry[]
}

export interface CheckDetailsViewProps {
  readonly data: CheckDetailsData | null
  readonly status: CheckDetailsStatus
  readonly activeTab: CheckDetailsTab
  readonly onTabChange: (tab: CheckDetailsTab) => void
  readonly onAction: (action: CheckAction) => void
  readonly onRetry: () => void
  /** Actions that have no backend yet and are rendered disabled with a hint. */
  readonly unavailableActions?: readonly CheckAction[]
  /** Current time injected by the caller so relative ages stay deterministic. */
  readonly now: Date
}
