import { useNavigate } from '@tanstack/react-router'
import { useEffect } from 'react'
import { onSessionExpired } from '../../shared/api/client'

export function useSessionExpiredRedirect(): void {
  const navigate = useNavigate()

  useEffect(
    () =>
      onSessionExpired(() => {
        void navigate({ to: '/login' })
      }),
    [navigate],
  )
}
