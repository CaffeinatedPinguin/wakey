import { Button } from '@mantine/core'
import type { ReactElement } from 'react'
import { appConfig } from '../../../shared/config'

interface LoginButtonProps {
  label: string
  fullWidth?: boolean
}

export function LoginButton({ label, fullWidth = false }: LoginButtonProps): ReactElement {
  return (
    <Button component="a" href={appConfig.loginUrl} fullWidth={fullWidth}>
      {label}
    </Button>
  )
}
