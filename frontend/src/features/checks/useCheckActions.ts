import { notifications } from '@mantine/notifications'
import { useQueryClient } from '@tanstack/react-query'
import type { TFunction } from 'i18next'
import { ApiError } from '../../shared/api/problem'
import { deleteCheck, pauseCheck, resumeCheck } from './api/checks.api'
import { checksQueryKeys } from './api/checks.queries'

export interface CheckActions {
  pause: (id: string) => Promise<void>
  resume: (id: string) => Promise<void>
  remove: (id: string) => Promise<void>
  // The list and the counts are invalidated together, once per finished series.
  refresh: () => void
}

export function useCheckActions(space: string): CheckActions {
  const queryClient = useQueryClient()
  return {
    pause: async (id) => {
      await pauseCheck(space, id)
    },
    resume: async (id) => {
      await resumeCheck(space, id)
    },
    remove: (id) => deleteCheck(space, id),
    refresh: () => {
      void queryClient.invalidateQueries({ queryKey: checksQueryKeys.all })
    },
  }
}

function errorKey(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 403) {
      return 'checks.page.actionForbidden'
    }
    if (error.status === 404) {
      return 'checks.page.actionNotFound'
    }
    if (error.code === 'network') {
      return 'checks.page.actionNetwork'
    }
  }
  return 'checks.page.bulkError'
}

export function showActionError(t: TFunction, error: unknown): void {
  notifications.show({ color: 'red', message: t(errorKey(error)) })
}
