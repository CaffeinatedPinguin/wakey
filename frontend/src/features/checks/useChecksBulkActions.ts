import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { collectCheckIds } from './api/checks.api'
import type { CheckApiFilters, CollectedCheckIds } from './api/checks.api'
import { useDeleteCheck, usePauseCheck, useResumeCheck } from './api/checks.queries'
import type { BulkAction, RowAction, SelectionState } from './list/types'

const MAX_BULK_PAGES = 100

interface CheckApiActions {
  pause: (id: string) => Promise<unknown>
  resume: (id: string) => Promise<unknown>
  delete: (id: string) => Promise<void>
}

interface BulkRequest {
  action: BulkAction
  selection: SelectionState
  space: string
  filters: CheckApiFilters
  signal: AbortSignal
  actions: CheckApiActions
}

interface MutationLike {
  mutateAsync: (variables: { slug: string; id: string }) => Promise<unknown>
}

interface DeleteMutationLike {
  mutateAsync: (variables: { slug: string; id: string }) => Promise<void>
}

export interface ChecksBulkActions {
  onBulkAction: (action: BulkAction, selection: SelectionState) => void
  onRowAction: (id: string, action: RowAction) => void
  message: string | null
  deleteOpen: boolean
  cancelDelete: () => void
  confirmDelete: (selection: SelectionState) => void
}

interface Execution {
  run: (action: BulkAction, selection: SelectionState) => Promise<void>
  message: string | null
  actions: CheckApiActions
}

function createApiActions(
  space: string,
  pause: MutationLike,
  resume: MutationLike,
  remove: DeleteMutationLike,
): CheckApiActions {
  return {
    pause: (id) => pause.mutateAsync({ slug: space, id }),
    resume: (id) => resume.mutateAsync({ slug: space, id }),
    delete: (id) => remove.mutateAsync({ slug: space, id }),
  }
}

async function collectIds(request: BulkRequest): Promise<CollectedCheckIds> {
  if (request.selection.mode === 'page') {
    return { ids: request.selection.ids, complete: true }
  }
  if (request.selection.mode === 'none') {
    return { ids: [], complete: true }
  }
  return collectCheckIds(request.space, request.filters, MAX_BULK_PAGES, request.signal)
}

async function applyBulk(action: BulkAction, ids: string[], api: CheckApiActions): Promise<void> {
  for (const id of ids) {
    await applyOne(action, id, api)
  }
}

async function applyOne(action: BulkAction, id: string, api: CheckApiActions): Promise<void> {
  switch (action) {
    case 'pause':
      await api.pause(id)
      return
    case 'resume':
      await api.resume(id)
      return
    case 'delete':
      await api.delete(id)
      return
    case 'mute':
    case 'tags':
    case 'policy':
      return
  }
}

async function executeBulk(request: BulkRequest): Promise<boolean> {
  const target = await collectIds(request)
  if (!target.complete) {
    return false
  }
  await applyBulk(request.action, target.ids, request.actions)
  return true
}

function useBulkExecution(space: string, filters: CheckApiFilters): Execution {
  const { t } = useTranslation()
  const pause = usePauseCheck()
  const resume = useResumeCheck()
  const remove = useDeleteCheck()
  const [message, setMessage] = useState<string | null>(null)
  const actions = createApiActions(space, pause, resume, remove)

  async function run(action: BulkAction, selection: SelectionState): Promise<void> {
    setMessage(null)
    const request = {
      action,
      selection,
      space,
      filters,
      signal: new AbortController().signal,
      actions,
    }
    try {
      const complete = await executeBulk(request)
      if (!complete) {
        setMessage(t('checks.page.bulkLimit'))
      }
    } catch {
      setMessage(t('checks.page.bulkError'))
    }
  }

  return { run, message, actions }
}

export function useChecksBulkActions(space: string, filters: CheckApiFilters): ChecksBulkActions {
  const execution = useBulkExecution(space, filters)
  const [deleteOpen, setDeleteOpen] = useState(false)

  return {
    onBulkAction: (action, selection) => {
      if (action === 'delete') {
        setDeleteOpen(true)
        return
      }
      void execution.run(action, selection)
    },
    onRowAction: (id, action) => {
      if (action === 'pause') {
        void execution.actions.pause(id)
      }
      if (action === 'resume') {
        void execution.actions.resume(id)
      }
    },
    message: execution.message,
    deleteOpen,
    cancelDelete: () => {
      setDeleteOpen(false)
    },
    confirmDelete: (selection) => {
      setDeleteOpen(false)
      void execution.run('delete', selection)
    },
  }
}
