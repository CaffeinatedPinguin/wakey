import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { collectCheckIds } from './api/checks.api'
import type { CheckApiFilters, CollectedCheckIds } from './api/checks.api'
import type { BulkAction, BulkProgress, RowAction, SelectionState } from './list/types'
import { showActionError, useCheckActions } from './useCheckActions'
import type { CheckActions } from './useCheckActions'

const MAX_BULK_CHECKS = 500

export interface ChecksBulkActions {
  onBulkAction: (action: BulkAction, selection: SelectionState) => void
  onRowAction: (id: string, action: RowAction) => void
  message: string | null
  running: boolean
  progress: BulkProgress | null
  onCancel: () => void
  deleteOpen: boolean
  deleteCount: number
  cancelDelete: () => void
  confirmDelete: () => void
}

type CollectSelection = (selection: SelectionState) => Promise<string[] | null>

type RunIds = (action: BulkAction, ids: string[]) => Promise<void>

async function resolveIds(
  space: string,
  filters: CheckApiFilters,
  selection: SelectionState,
): Promise<CollectedCheckIds> {
  if (selection.mode === 'all') {
    return collectCheckIds(space, filters, MAX_BULK_CHECKS)
  }
  if (selection.mode === 'page') {
    return { ids: selection.ids, complete: true }
  }
  return { ids: [], complete: true }
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

interface RunContext {
  actions: CheckActions
  report: (done: number, total: number) => void
  isCancelled: () => boolean
}

async function applyIds(
  action: BulkAction,
  ids: readonly string[],
  context: RunContext,
): Promise<void> {
  context.report(0, ids.length)
  let done = 0
  for (const id of ids) {
    if (context.isCancelled()) {
      return
    }
    await applyOne(action, id, context.actions)
    done += 1
    context.report(done, ids.length)
  }
}

function useBulkStatus() {
  const [message, setMessage] = useState<string | null>(null)
  const [running, setRunning] = useState(false)
  const [progress, setProgress] = useState<BulkProgress | null>(null)
  const busy = useRef(false)

  return {
    message,
    running,
    progress,
    acquire: (): boolean => {
      if (busy.current) {
        return false
      }
      busy.current = true
      return true
    },
    release: (): void => {
      busy.current = false
    },
    begin: () => {
      setMessage(null)
      setRunning(true)
      setProgress(null)
    },
    finish: () => {
      setRunning(false)
      setProgress(null)
    },
    fail: setMessage,
    report: setProgress,
  }
}

function useBulkCollector(
  space: string,
  filters: CheckApiFilters,
  status: ReturnType<typeof useBulkStatus>,
): CollectSelection {
  const { t } = useTranslation()

  return async (selection) => {
    if (!status.acquire()) {
      return null
    }
    status.begin()
    try {
      const target = await resolveIds(space, filters, selection)
      if (!target.complete) {
        status.fail(t('checks.page.bulkLimit'))
        return null
      }
      return target.ids
    } catch (error) {
      showActionError(t, error)
      return null
    } finally {
      status.release()
      status.finish()
    }
  }
}

function useRunIds(
  actions: CheckActions,
  status: ReturnType<typeof useBulkStatus>,
  onFinished: () => void,
) {
  const { t } = useTranslation()
  const cancelled = useRef(false)

  async function runIds(action: BulkAction, ids: string[]): Promise<void> {
    if (!status.acquire()) {
      return
    }
    cancelled.current = false
    status.begin()
    try {
      await applyIds(action, ids, {
        actions,
        report: (done, total) => {
          status.report({ done, total })
        },
        isCancelled: () => cancelled.current,
      })
    } catch (error) {
      showActionError(t, error)
    } finally {
      status.release()
      status.finish()
      actions.refresh()
      onFinished()
    }
  }

  return {
    runIds,
    cancel: () => {
      cancelled.current = true
    },
  }
}

function useRunSelection(collect: CollectSelection, runIds: RunIds) {
  return async (action: BulkAction, selection: SelectionState): Promise<void> => {
    const ids = await collect(selection)
    if (ids !== null) {
      await runIds(action, ids)
    }
  }
}

function useRowRunner(actions: CheckActions) {
  const { t } = useTranslation()

  return async (id: string, action: RowAction) => {
    try {
      await applyOne(action, id, actions)
    } catch (error) {
      showActionError(t, error)
    } finally {
      actions.refresh()
    }
  }
}

function useDeleteFlow(collect: CollectSelection, runIds: RunIds) {
  const [ids, setIds] = useState<string[] | null>(null)

  return {
    ids,
    prepare: async (selection: SelectionState) => {
      const collected = await collect(selection)
      if (collected !== null) {
        setIds(collected)
      }
    },
    cancel: () => {
      setIds(null)
    },
    confirm: () => {
      const current = ids
      if (current === null) {
        return
      }
      setIds(null)
      void runIds('delete', current)
    },
  }
}

export function useChecksBulkActions(
  space: string,
  filters: CheckApiFilters,
  onFinished: () => void,
): ChecksBulkActions {
  const actions = useCheckActions(space)
  const status = useBulkStatus()
  const collect = useBulkCollector(space, filters, status)
  const ids = useRunIds(actions, status, onFinished)
  const run = useRunSelection(collect, ids.runIds)
  const deletion = useDeleteFlow(collect, ids.runIds)
  const runRow = useRowRunner(actions)

  return {
    onBulkAction: (action, selection) => {
      if (action === 'delete') {
        void deletion.prepare(selection)
        return
      }
      void run(action, selection)
    },
    onRowAction: (id, action) => {
      void runRow(id, action)
    },
    message: status.message,
    running: status.running,
    progress: status.progress,
    onCancel: ids.cancel,
    deleteOpen: deletion.ids !== null,
    deleteCount: deletion.ids?.length ?? 0,
    cancelDelete: deletion.cancel,
    confirmDelete: deletion.confirm,
  }
}
