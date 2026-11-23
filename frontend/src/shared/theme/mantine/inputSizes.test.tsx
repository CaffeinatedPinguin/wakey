import { MantineProvider, TextInput, type MantineSize } from '@mantine/core'
import { cleanup, render } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { uptimeExtension } from '../extensions/uptime'
import { createAppTheme } from './createAppTheme'

const { theme, cssVariablesResolver } = createAppTheme({
  extensions: [uptimeExtension],
  labels: { close: 'test-close' },
})

const CASES: readonly (readonly [MantineSize, string])[] = [
  ['xs', 'compact'],
  ['sm', 'desktop'],
  ['md', 'desktop'],
  ['lg', 'large'],
  ['xl', 'touch'],
]

afterEach(() => {
  cleanup()
})

function renderInput(size: MantineSize): Element | null {
  const view = render(
    <MantineProvider
      theme={theme}
      cssVariablesResolver={cssVariablesResolver}
      forceColorScheme="light"
    >
      <TextInput size={size} label="Name" />
    </MantineProvider>,
  )
  return view.container.querySelector('.mantine-Input-wrapper')
}

describe('input sizes', () => {
  it('input sizes follow the theme on every size', () => {
    for (const [size, token] of CASES) {
      const wrapper = renderInput(size)
      const style = wrapper?.getAttribute('style') ?? ''
      expect(style).toContain(`--input-height-${size}: var(--app-size-control-${token})`)
      cleanup()
    }
  })
})
