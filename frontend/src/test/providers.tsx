import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { AppThemeProvider } from '../app/App'
import '../app/i18n'

export function renderWithProviders(ui: ReactElement): ReturnType<typeof render> {
  return render(<AppThemeProvider>{ui}</AppThemeProvider>)
}
