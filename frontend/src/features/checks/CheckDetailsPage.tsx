import { useNavigate, useParams, useSearch } from '@tanstack/react-router'
import type { TFunction } from 'i18next'
import type { ReactElement } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { isNotFoundError } from '../../shared/api/client'
import { toCheckDetails } from './api/checks.adapters'
import { useCheck } from './api/checks.queries'
import { spaceOf } from './api/checks.search'
import { CheckDetailsView } from './details/CheckDetailsView'
import type { CheckAction, CheckDetailsStatus, CheckDetailsTab } from './details/checkDetailsTypes'
import { CheckDetailsMissingSpace, CheckDetailsNotFound } from './details/CheckDetailsStates'
import { showActionError, useCheckActions } from './useCheckActions'
import type { CheckActions } from './useCheckActions'

const UNAVAILABLE_ACTIONS: readonly CheckAction[] = ['check-now', 'edit', 'duplicate']

function detailStatus(query: ReturnType<typeof useCheck>): CheckDetailsStatus {
  if (query.isPending) {
    return 'loading'
  }
  if (query.isError) {
    return isNotFoundError(query.error) ? 'notFound' : 'error'
  }
  return 'ready'
}

interface CheckDetailsContentProps {
  space: string
  checkId: string
}

interface DetailActionContext {
  checkId: string
  actions: CheckActions
  goToList: () => void
  t: TFunction
}

async function performAction(action: CheckAction, context: DetailActionContext): Promise<boolean> {
  switch (action) {
    case 'pause':
      await context.actions.pause(context.checkId)
      return true
    case 'resume':
      await context.actions.resume(context.checkId)
      return true
    case 'delete':
      await context.actions.remove(context.checkId)
      return true
    case 'check-now':
    case 'edit':
    case 'duplicate':
      return false
  }
}

async function runDetailAction(action: CheckAction, context: DetailActionContext): Promise<void> {
  try {
    const changed = await performAction(action, context)
    if (!changed) {
      return
    }
    context.actions.refresh()
    if (action === 'delete') {
      context.goToList()
    }
  } catch (error) {
    showActionError(context.t, error)
    context.actions.refresh()
  }
}

function CheckDetailsContent({ space, checkId }: CheckDetailsContentProps): ReactElement {
  const { t } = useTranslation()
  const query = useCheck(space, checkId)
  const actions = useCheckActions(space)
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<CheckDetailsTab>('events')
  const status = detailStatus(query)
  const data = query.data === undefined ? null : toCheckDetails(query.data)
  const reloadCheck = query.refetch
  const goToList = (): void => {
    void navigate({ to: '/checks', search: (previous) => ({ ...previous, space }) })
  }
  const context = { checkId, actions, goToList, t }

  return (
    <CheckDetailsView
      data={data}
      status={status}
      activeTab={activeTab}
      onTabChange={setActiveTab}
      onAction={(action) => {
        void runDetailAction(action, context)
      }}
      onRetry={() => {
        void reloadCheck()
      }}
      unavailableActions={UNAVAILABLE_ACTIONS}
      now={new Date()}
    />
  )
}

export function CheckDetailsPage(): ReactElement {
  const params = useParams({ strict: false })
  const search = useSearch({ strict: false })
  const checkId = typeof params.checkId === 'string' ? params.checkId : undefined
  const space = spaceOf(search)
  if (space === undefined) {
    return <CheckDetailsMissingSpace />
  }
  if (checkId === undefined) {
    return <CheckDetailsNotFound />
  }
  return <CheckDetailsContent space={space} checkId={checkId} />
}
