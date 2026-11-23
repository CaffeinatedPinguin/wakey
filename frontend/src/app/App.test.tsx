import { cleanup, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { renderApp } from '../test/renderApp'

afterEach(() => {
  cleanup()
})

describe('App', () => {
  it('renders the wakey heading', async () => {
    renderApp('/login')

    const heading = await screen.findByRole('heading', { level: 1 })

    expect(heading.textContent).toBe('wakey')
  })
})
