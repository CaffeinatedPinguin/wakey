export interface SessionData {
  readonly id: string
  readonly device: string
  readonly browser: string
  readonly lastActive: string
  readonly location?: string
  readonly current: boolean
}

export type SessionsStatus = 'loading' | 'ready' | 'empty' | 'error'

export interface SessionsViewProps {
  readonly sessions: readonly SessionData[]
  readonly status: SessionsStatus
  readonly pendingIds: readonly string[]
  readonly onLogout: (id: string) => void
  readonly onLogoutAll: () => void
  readonly onRetry: () => void
  readonly now?: Date
}

export interface SessionRowProps {
  readonly session: SessionData
  readonly pending: boolean
  readonly now: Date
  readonly onLogout: (id: string) => void
}

export interface SessionCardProps {
  readonly session: SessionData
  readonly pending: boolean
  readonly now: Date
  readonly onLogout: (id: string) => void
}

export interface SessionListProps {
  readonly sessions: readonly SessionData[]
  readonly pendingIds: readonly string[]
  readonly now: Date
  readonly onLogout: (id: string) => void
}

export interface SessionsErrorProps {
  readonly onRetry: () => void
}

export interface LogoutAllDialogProps {
  readonly opened: boolean
  readonly sessionCount: number
  readonly onConfirm: () => void
  readonly onClose: () => void
}

export interface NewTokenSecretBannerProps {
  readonly name: string
  readonly secret: string
  readonly onCopy: () => void
  readonly onDismiss: () => void
}
