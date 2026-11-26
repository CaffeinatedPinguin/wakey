import { readdirSync, readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const SOURCE_ROOT = `${process.cwd()}/src`
const GENERATED_DIRECTORY = `${SOURCE_ROOT}/shared/api/generated`
const ALLOWED_FILES = [
  `${SOURCE_ROOT}/shared/api/contract.test.ts`,
  `${SOURCE_ROOT}/test/exceptionNaming.test.ts`,
]
const FORBIDDEN_WORD = /problem(?!Detail)/i

function isSourceFile(name: string): boolean {
  return name.endsWith('.ts') || name.endsWith('.tsx')
}

function collectSourceFiles(directory: string): string[] {
  const files: string[] = []
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const path = `${directory}/${entry.name}`
    if (entry.isDirectory()) {
      files.push(...collectSourceFiles(path))
    } else if (isSourceFile(entry.name)) {
      files.push(path)
    }
  }
  return files
}

function isAllowed(path: string): boolean {
  return path.startsWith(`${GENERATED_DIRECTORY}/`) || ALLOWED_FILES.includes(path)
}

function mentionsForbiddenWord(path: string): boolean {
  return FORBIDDEN_WORD.test(path) || FORBIDDEN_WORD.test(readFileSync(path, 'utf8'))
}

describe('the API exception naming', () => {
  it('no frontend identifier or file outside the generated client mentions problem', () => {
    // given
    const sourceFiles = collectSourceFiles(SOURCE_ROOT)

    // when
    const offenders = sourceFiles.filter((path) => !isAllowed(path) && mentionsForbiddenWord(path))

    // then
    expect(offenders).toEqual([])
  })
})
