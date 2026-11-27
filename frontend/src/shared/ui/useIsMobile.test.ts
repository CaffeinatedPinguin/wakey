import { existsSync, readdirSync, readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

// The one mobile edge (just below the Mantine `sm` breakpoint) lives in the
// theme-driven shared hook. Any other file that hard-codes it is a copy.
const THRESHOLD = ['47', '99'].join('.')
const SOURCE_EXTENSIONS = ['.ts', '.tsx', '.css']
const IGNORED = ['shared/theme/', 'shared/ui/useIsMobile.ts', 'shared/ui/useIsMobile.test.ts']
const ROOT_CANDIDATES = [`${process.cwd()}/src`, `${process.cwd()}/frontend/src`]

function sourceRoot(): string {
  return ROOT_CANDIDATES.find((candidate) => existsSync(candidate)) ?? ''
}

function normalise(path: string): string {
  return path.split('\\').join('/')
}

function isSourceFile(relative: string): boolean {
  const normalized = normalise(relative)
  const ignored = IGNORED.some((entry) => normalized.includes(entry))
  const isSource = SOURCE_EXTENSIONS.some((extension) => normalized.endsWith(extension))
  return isSource && !ignored
}

function filesWithCopiedThreshold(): string[] {
  const root = sourceRoot()
  return readdirSync(root, { recursive: true })
    .map(normalise)
    .filter(isSourceFile)
    .filter((relative) => readFileSync(`${root}/${relative}`, 'utf8').includes(THRESHOLD))
}

describe('mobile breakpoint', () => {
  it('the mobile breakpoint is defined in one place', () => {
    expect(filesWithCopiedThreshold()).toEqual([])
  })
})
