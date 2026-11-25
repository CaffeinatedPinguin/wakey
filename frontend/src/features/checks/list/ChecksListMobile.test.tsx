import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import { afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest'
import i18n from '../../../app/i18n'
import { coreTokens } from '../../../shared/theme'
import { installChecksTranslations } from '../../../test/checksListI18n'
import { installViewport } from '../../../test/viewport'
import { renderChecksList } from './testSupport'

const DESKTOP_WIDTH = 1024
const MOBILE_WIDTH = 375
const PIXELS_PER_REM = 16

beforeAll(async () => {
  await installChecksTranslations()
})

beforeEach(() => {
  installViewport(DESKTOP_WIDTH)
})

afterEach(async () => {
  cleanup()
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

describe('checks list interactions', () => {
  it('on mobile the checks are cards with touch targets of 44 px and filters in a sheet', async () => {
    installViewport(MOBILE_WIDTH)
    renderChecksList()

    expect(screen.queryByRole('table')).toBeNull()

    const card = screen.getByRole('article', { name: 'API płatności' })
    const touchToken = coreTokens.shared['--app-size-control-touch'] ?? ''
    const touchPixels = Number.parseFloat(touchToken) * PIXELS_PER_REM

    expect(touchPixels).toBe(44)
    expect(card.style.minHeight).toBe('var(--app-size-control-touch)')

    fireEvent.click(screen.getByRole('button', { name: 'Filtry' }))
    const sheet = await screen.findByRole('dialog', { name: 'Filtry' })

    expect(within(sheet).getByText('7')).toBeDefined()
  })

  it('a row action reports the row id and the action', async () => {
    const result = renderChecksList()

    fireEvent.click(screen.getByRole('button', { name: 'Akcje checka API płatności' }))
    fireEvent.click(await screen.findByRole('menuitem', { name: 'Pauza' }))

    expect(result.viewProps.onRowAction).toHaveBeenCalledWith('c1', 'pause')
  })
})
