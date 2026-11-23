import { Modal } from '@mantine/core'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { AppThemeProvider } from './App'
import i18n from './i18n'

function renderModal(): void {
  render(
    <AppThemeProvider>
      <Modal opened onClose={() => undefined} title="Test">
        Body
      </Modal>
    </AppThemeProvider>,
  )
}

afterEach(async () => {
  cleanup()
  await i18n.changeLanguage('pl')
})

describe('modal labels', () => {
  it('the close label comes from the current language', async () => {
    await i18n.changeLanguage('en')
    renderModal()
    expect(await screen.findByRole('button', { name: 'Close' })).toBeDefined()
    cleanup()

    await i18n.changeLanguage('pl')
    renderModal()
    expect(await screen.findByRole('button', { name: 'Zamknij' })).toBeDefined()
  })
})
