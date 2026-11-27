import { notifications } from '@mantine/notifications'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { TFunction } from 'i18next'
import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiException } from '../../shared/api/apiException'
import { BulkCheckAction, ErrorCode } from '../../shared/api/generated/enums'
import type { BulkChecksRequest } from '../../shared/api/generated/types'
import { bulkChecks } from './api/checks.api'
import type { CheckApiFilters } from './api/checks.api'
import { checksQueryKeys } from './api/checks.queries'
import { SelectionMode } from './list/types'
import type { SelectionState } from './list/types'
import { applyRow } from './rowActions'
import { showActionError, useCheckActions } from './useCheckActions'
import type { CheckActions } from './useCheckActions'

export interface ChecksBulkActions {
  onBulkAction: (action: BulkCheckAction, selection: SelectionState) => void
  onRowAction: (id: string, action: BulkCheckAction) => void
  message: string | null
  running: boolean
  deleteOpen: boolean
  deleteCount: number
  cancelDelete: () => void
  confirmDelete: () => void
}

interface PendingDelete {
  request: BulkChecksRequest
  count: number
}

function requestOf(
  action: BulkCheckAction,
  selection: SelectionState,
  filters: CheckApiFilters,
  expectedCount: number,
): BulkChecksRequest {
  if (selection.mode === SelectionMode.ALL) {
    return { action, filter: filters, expectedCount }
  }
  return { action, ids: selection.ids }
}

function deleteCount(selection: SelectionState, totalCount: number): number {
  return selection.mode === SelectionMode.ALL ? totalCount : selection.ids.length
}

interface RunRowDeps {
  actions: CheckActions
  busy: { current: boolean }
  t: TFunction
}

function runRow(id: string, action: BulkCheckAction, deps: RunRowDeps): void {
  if (deps.busy.current) {
    return
  }
  deps.busy.current = true
  void applyRow(action, id, deps.actions)
    .catch((error: unknown) => {
      showActionError(deps.t, error)
    })
    .finally(() => {
      deps.busy.current = false
      deps.actions.refresh()
    })
}

function showBulkError(t: TFunction, error: unknown): void {
  if (error instanceof ApiException && error.code === ErrorCode.BULK_COUNT_CHANGED) {
    notifications.show({
      color: 'red',
      message: t('errors.bulk_count_changed', {
        expected: error.params.expected,
        actual: error.params.actual,
      }),
    })
    return
  }
  showActionError(t, error)
}

function useBulkMutation(space: string, onFinished: () => void) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const busy = useRef(false)
  const [message, setMessage] = useState<string | null>(null)
  const mutation = useMutation({
    mutationFn: (request: BulkChecksRequest) => bulkChecks(space, request),
    onSuccess: (result) => {
      setMessage(t('checks.page.bulkAffected', { count: result.affected }))
      onFinished()
    },
    onError: (error) => {
      showBulkError(t, error)
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: checksQueryKeys.all })
    },
  })

  function start(request: BulkChecksRequest): void {
    if (busy.current) {
      return
    }
    busy.current = true
    setMessage(null)
    mutation.mutate(request, {
      onSettled: () => {
        busy.current = false
      },
    })
  }

  return { message, running: mutation.isPending, busy, start }
}

interface DeleteFlowDeps {
  filters: CheckApiFilters
  totalCount: number
  start: (request: BulkChecksRequest) => void
}

function useDeleteFlow(deps: DeleteFlowDeps) {
  const [pending, setPending] = useState<PendingDelete | null>(null)

  function open(action: BulkCheckAction, selection: SelectionState): void {
    const count = deleteCount(selection, deps.totalCount)
    setPending({
      request: requestOf(action, selection, deps.filters, count),
      count,
    })
  }

  function confirm(): void {
    if (pending === null) {
      return
    }
    setPending(null)
    deps.start(pending.request)
  }

  return {
    pending,
    open,
    confirm,
    cancel: () => {
      setPending(null)
    },
  }
}

function useRowAction(actions: CheckActions, busy: { current: boolean }) {
  const { t } = useTranslation()
  return (id: string, action: BulkCheckAction): void => {
    runRow(id, action, { actions, busy, t })
  }
}

export function useChecksBulkActions(
  space: string,
  filters: CheckApiFilters,
  totalCount: number,
  onFinished: () => void,
): ChecksBulkActions {
  const actions = useCheckActions(space)
  const bulk = useBulkMutation(space, onFinished)
  const deletion = useDeleteFlow({ filters, totalCount, start: bulk.start })
  const onRowAction = useRowAction(actions, bulk.busy)

  function onBulkAction(action: BulkCheckAction, selection: SelectionState): void {
    if (action === BulkCheckAction.DELETE) {
      deletion.open(action, selection)
      return
    }
    bulk.start(requestOf(action, selection, filters, deleteCount(selection, totalCount)))
  }

  return {
    onBulkAction,
    onRowAction,
    message: bulk.message,
    running: bulk.running,
    deleteOpen: deletion.pending !== null,
    deleteCount: deletion.pending?.count ?? 0,
    cancelDelete: deletion.cancel,
    confirmDelete: deletion.confirm,
  }
}
