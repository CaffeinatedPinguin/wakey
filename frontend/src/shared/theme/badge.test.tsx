import { Badge, MantineProvider } from '@mantine/core'
import { cleanup, render } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { createAppTheme } from './mantine/createAppTheme'
import { uptimeExtension } from './extensions/uptime'

const { theme, cssVariablesResolver } = createAppTheme({
  extensions: [uptimeExtension],
  labels: { close: 'test-close' },
})

afterEach(() => {
  cleanup()
})

describe('state badge', () => {
  it('a state badge uses the state tokens in both schemes', () => {
    for (const scheme of ['light', 'dark'] as const) {
      const view = render(
        <MantineProvider
          theme={theme}
          cssVariablesResolver={cssVariablesResolver}
          forceColorScheme={scheme}
        >
          <Badge color="down">Down</Badge>
        </MantineProvider>,
      )
      const badge = view.getByText('Down').parentElement
      const style = badge?.getAttribute('style') ?? ''
      expect(style).toContain('--badge-bg: var(--app-state-down-bg)')
      expect(style).toContain('--badge-color: var(--app-state-down-text)')
      cleanup()
    }
  })
})
