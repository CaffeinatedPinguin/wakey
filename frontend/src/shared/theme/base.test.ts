import { existsSync, readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

// Vitest stubs `?raw` CSS imports, so the stylesheet is read from disk. The
// gate runs the frontend package as the working directory.
const baseCssPath = [
  `${process.cwd()}/src/shared/theme/base.css`,
  `${process.cwd()}/frontend/src/shared/theme/base.css`,
].find((candidate) => existsSync(candidate))
const baseCss = readFileSync(baseCssPath ?? '', 'utf8')

const touchQuery = '@media (max-width: 63.99em), (pointer: coarse)'

function closingIndex(css: string, start: number): number {
  let depth = 0
  for (let index = start; index < css.length; index += 1) {
    depth += Number(css[index] === '{') - Number(css[index] === '}')
    if (depth === 0 && css[index] === '}') {
      return index
    }
  }

  return -1
}

function mediaBlock(css: string, query: string): string {
  const start = css.indexOf(query)
  expect(start).toBeGreaterThanOrEqual(0)

  const open = css.indexOf('{', start)
  const close = closingIndex(css, open)
  expect(close).toBeGreaterThan(open)

  return css.slice(start, close + 1)
}

describe('base.css', () => {
  it('the used colour scheme follows the app toggle, not the system', () => {
    expect(baseCss).not.toMatch(/color-scheme\s*:/)
  })

  it('touch rules cover segmented controls and icon-only buttons', () => {
    const touch = mediaBlock(baseCss, touchQuery)

    expect(touch).toMatch(
      /\.mantine-SegmentedControl-label[^{]*\{[^}]*min-height:\s*var\(--app-size-control-touch\)/,
    )
    expect(touch).toMatch(
      /\.mantine-ActionIcon-root[^{]*\{[^}]*min-width:\s*var\(--app-size-control-touch\)/,
    )
    expect(touch).toMatch(
      /\.mantine-Button-root\[aria-label\][^{]*\{[^}]*min-width:\s*var\(--app-size-control-touch\)/,
    )
  })
})
