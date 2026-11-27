import { readdirSync, readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const SOURCE_ROOT = `${process.cwd()}/src`
const GENERATED_DIRECTORY = `${SOURCE_ROOT}/shared/api/generated`
const ALLOWED_FILES = [
  `${SOURCE_ROOT}/shared/api/contract.test.ts`,
  `${SOURCE_ROOT}/test/exceptionNaming.test.ts`,
]
const FORBIDDEN_WORD = /problem(?!Detail)/i
const IDENTIFIER = /[A-Za-z_$][A-Za-z0-9_$]*/g
const COMMENTS_AND_STRINGS =
  /\/\*[\s\S]*?\*\/|\/\/[^\n\r]*|'(?:\\.|[^'\\])*'|"(?:\\.|[^"\\])*"|`(?:\\.|[^`\\])*`/g

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

function codeIdentifiers(source: string): string[] {
  return source.replace(COMMENTS_AND_STRINGS, ' ').match(IDENTIFIER) ?? []
}

function fileNameOf(path: string): string {
  const fileName = path.slice(path.lastIndexOf('/') + 1)
  return fileName.slice(0, fileName.lastIndexOf('.'))
}

function mentionsForbiddenWord(path: string): boolean {
  const identifiers = codeIdentifiers(readFileSync(path, 'utf8'))
  return [fileNameOf(path), ...identifiers].some((identifier) => FORBIDDEN_WORD.test(identifier))
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
