import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { collectCheckIds } from './api/checks.api'
import type { CheckApiFilters, CollectedCheckIds } from './api/checks.api'
import type { BulkAction, RowAction, SelectionState } from './list/types'
import { showActionError, useCheckActions } from './useCheckActions'
import type { CheckActions } from './useCheckActions'

const MAX_BULK_PAGES = 100

interface BulkRequest {
  action: BulkAction
  selection: SelectionState
  space: string
  filters: CheckApiFilters
  actions: CheckActions
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
  runOne: (id: string, action: RowAction) => Promise<void>
  message: string | null
}

async function collectIds(request: BulkRequest): Promise<CollectedCheckIds> {
  if (request.selection.mode === 'page') {
    return { ids: request.selection.ids, complete: true }
  }
  if (request.selection.mode === 'none') {
    return { ids: [], complete: true }
  }
  return collectCheckIds(request.space, request.filters, MAX_BULK_PAGES)
}

async function applyOne(action: BulkAction, id: string, api: CheckActions): Promise<void> {
  switch (action) {
    case 'pause':
      await api.pause(id)
      return
    case 'resume':
      await api.resume(id)
      return
    case 'delete':
      await api.remove(id)
      return
  }
}

async function applyBulk(action: BulkAction, ids: string[], api: CheckActions): Promise<void> {
  for (const id of ids) {
    await applyOne(action, id, api)
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
  const actions = useCheckActions(space)
  const [message, setMessage] = useState<string | null>(null)

  async function run(action: BulkAction, selection: SelectionState): Promise<void> {
    setMessage(null)
    try {
      const complete = await executeBulk({ action, selection, space, filters, actions })
      if (!complete) {
        setMessage(t('checks.page.bulkLimit'))
      }
    } catch (error) {
      showActionError(t, error)
    } finally {
      actions.refresh()
    }
  }

  async function runOne(id: string, action: RowAction): Promise<void> {
    try {
      await applyOne(action, id, actions)
    } catch (error) {
      showActionError(t, error)
    } finally {
      actions.refresh()
    }
  }

  return { run, runOne, message }
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
      void execution.runOne(id, action)
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
