import { useNavigate, useParams, useSearch } from '@tanstack/react-router'
import type { TFunction } from 'i18next'
import type { ReactElement } from 'react'
import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { isNotFoundException } from '../../shared/api/client'
import { BulkCheckAction } from '../../shared/api/generated/enums'
import { toCheckDetails } from './api/checks.adapters'
import { useCheck } from './api/checks.queries'
import { CheckDetailsView } from './details/CheckDetailsView'
import { CheckDetailsMissingSpace } from './details/CheckDetailsStates'
import { applyRow } from './rowActions'
import { showActionError, useCheckActions } from './useCheckActions'
import type { CheckActions } from './useCheckActions'

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

async function runDetailAction(
  action: BulkCheckAction,
  context: DetailActionContext,
): Promise<void> {
  try {
    await applyRow(action, context.checkId, context.actions)
    context.actions.refresh()
    if (action === BulkCheckAction.DELETE) {
      context.goToList()
    }
  } catch (error) {
    showActionError(context.t, error)
    context.actions.refresh()
  }
}

interface DetailRunner {
  busy: boolean
  runAction: (action: BulkCheckAction) => void
}

function useDetailRunner(context: DetailActionContext): DetailRunner {
  const [busy, setBusy] = useState(false)
  const busyRef = useRef(false)
  const runAction = (action: BulkCheckAction): void => {
    if (busyRef.current) {
      return
    }
    busyRef.current = true
    setBusy(true)
    void runDetailAction(action, context).finally(() => {
      busyRef.current = false
      setBusy(false)
    })
  }
  return { busy, runAction }
}

function CheckDetailsContent({ space, checkId }: CheckDetailsContentProps): ReactElement {
  const { t } = useTranslation()
  const query = useCheck(space, checkId)
  const actions = useCheckActions(space)
  const navigate = useNavigate()
  const data = query.data === undefined ? null : toCheckDetails(query.data)
  const notFound = query.status === 'error' && isNotFoundException(query.error)
  const reloadCheck = query.refetch
  const goToList = (): void => {
    void navigate({ to: '/checks', search: (previous) => ({ ...previous, space }) })
  }
  const runner = useDetailRunner({ checkId, actions, goToList, t })

  return (
    <CheckDetailsView
      data={data}
      status={query.status}
      notFound={notFound}
      onAction={runner.runAction}
      busy={runner.busy}
      onRetry={() => {
        void reloadCheck()
      }}
      now={new Date()}
    />
  )
}

export function CheckDetailsPage(): ReactElement {
  const { checkId } = useParams({ from: '/panel/checks/$checkId' })
  const { space } = useSearch({ from: '/panel/checks/$checkId' })
  if (space === undefined) {
    return <CheckDetailsMissingSpace />
  }
  return <CheckDetailsContent space={space} checkId={checkId} />
}
