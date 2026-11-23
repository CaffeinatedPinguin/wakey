import { useRef } from 'react'
import { useLogout } from '../../features/auth'

export interface PanelLogout {
  requestLogout: () => void
  isPending: boolean
}

// A synchronous latch works before the mutation state updates, so a very fast
// second click cannot send a second sign-out request.
export function usePanelLogout(): PanelLogout {
  const logoutMutation = useLogout()
  const started = useRef(false)

  function requestLogout(): void {
    if (started.current) {
      return
    }
    started.current = true
    logoutMutation.mutate(undefined, {
      onError: () => {
        started.current = false
      },
    })
  }

  return { requestLogout, isPending: logoutMutation.isPending }
}
