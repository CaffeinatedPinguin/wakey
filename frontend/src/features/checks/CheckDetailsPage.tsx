import { useNavigate, useParams, useSearch } from '@tanstack/react-router'
import type { ReactElement } from 'react'
import { useState } from 'react'
import { isNotFoundError } from '../../shared/api/client'
import { toCheckDetails } from './api/checks.adapters'
import { useCheck, useDeleteCheck, usePauseCheck, useResumeCheck } from './api/checks.queries'
import { spaceOf } from './api/checks.search'
import { CheckDetailsView } from './details/CheckDetailsView'
import type { CheckAction, CheckDetailsStatus, CheckDetailsTab } from './details/checkDetailsTypes'
import { CheckDetailsLoading } from './details/CheckDetailsStates'

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

interface MutationLike {
  mutateAsync: (variables: { slug: string; id: string }) => Promise<unknown>
}

interface DeleteMutationLike {
  mutateAsync: (variables: { slug: string; id: string }) => Promise<void>
}

interface DetailActionContext {
  space: string
  checkId: string
  pause: MutationLike
  resume: MutationLike
  remove: DeleteMutationLike
  goToList: () => void
}

async function runDetailAction(action: CheckAction, context: DetailActionContext): Promise<void> {
  switch (action) {
    case 'pause':
      await context.pause.mutateAsync({ slug: context.space, id: context.checkId })
      return
    case 'resume':
      await context.resume.mutateAsync({ slug: context.space, id: context.checkId })
      return
    case 'delete':
      await context.remove.mutateAsync({ slug: context.space, id: context.checkId })
      context.goToList()
      return
    case 'check-now':
    case 'edit':
    case 'duplicate':
      return
  }
}

function CheckDetailsContent({ space, checkId }: CheckDetailsContentProps): ReactElement {
  const query = useCheck(space, checkId)
  const pause = usePauseCheck()
  const resume = useResumeCheck()
  const remove = useDeleteCheck()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<CheckDetailsTab>('events')
  const status = detailStatus(query)
  const data = query.data === undefined ? null : toCheckDetails(query.data)
  const reloadCheck = query.refetch
  const goToList = (): void => {
    void navigate({ to: '/checks', search: (previous) => ({ ...previous, space }) })
  }
  const context = { space, checkId, pause, resume, remove, goToList }

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
  if (space === undefined || checkId === undefined) {
    return <CheckDetailsLoading />
  }
  return <CheckDetailsContent space={space} checkId={checkId} />
}
