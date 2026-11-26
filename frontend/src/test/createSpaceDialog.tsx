import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import type { ReactElement } from 'react'
import { vi } from 'vitest'
import { AppThemeProvider } from '../app/App'
import { CreateSpaceDialog } from '../features/spaces/components/CreateSpaceDialog'
import type { CreateSpaceDialogProps } from '../features/spaces/components/CreateSpaceDialog'

export function renderCreateSpaceDialog(overrides: Partial<CreateSpaceDialogProps> = {}): {
  onCreated: ReturnType<typeof vi.fn>
  rerender: (opened: boolean) => void
} {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const onCreated = vi.fn()
  const props: CreateSpaceDialogProps = {
    opened: true,
    onClose: vi.fn(),
    onCreated,
    ...overrides,
  }
  const tree = (opened: boolean): ReactElement => (
    <AppThemeProvider env="test">
      <QueryClientProvider client={queryClient}>
        <CreateSpaceDialog {...props} opened={opened} />
      </QueryClientProvider>
    </AppThemeProvider>
  )
  const { rerender } = render(tree(true))
  return {
    onCreated: props.onCreated as ReturnType<typeof vi.fn>,
    rerender: (opened) => {
      rerender(tree(opened))
    },
  }
}

export function nameInput(): HTMLInputElement {
  return screen.getByLabelText<HTMLInputElement>('Nazwa')
}

export function slugInput(): HTMLInputElement {
  return screen.getByLabelText<HTMLInputElement>('Slug')
}
