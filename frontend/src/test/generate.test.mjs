import { spawnSync } from 'node:child_process'
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'

const frontendDir = process.cwd()
const scriptPath = join(frontendDir, 'scripts', 'generate-api.mjs')
const contractPath = join(frontendDir, '..', 'api', 'openapi', 'public.json')

// Each test spawns several `node scripts/generate-api.mjs` processes. Under a
// loaded machine (parallel builds) that can exceed the 5000 ms Vitest default,
// so give every spawning test an explicit ceiling well below the 120 s gate.
const GENERATOR_TEST_TIMEOUT_MS = 60_000

const createdDirs = []

function temporaryDirectory() {
  const directory = mkdtempSync(join(tmpdir(), 'wakey-api-'))
  createdDirs.push(directory)
  return directory
}

function runGenerator(argumentsList) {
  const result = spawnSync(process.execPath, [scriptPath, ...argumentsList], { encoding: 'utf8' })
  expect(result.error).toBeUndefined()
  return result
}

afterEach(() => {
  while (createdDirs.length > 0) {
    rmSync(createdDirs.pop(), { recursive: true, force: true })
  }
})

describe('API generator', () => {
  it(
    'the generator output is deterministic and check mode passes on fresh output',
    () => {
      const outDir = temporaryDirectory()

      const generated = runGenerator(['--contract', contractPath, '--out', outDir])

      expect(generated.status).toBe(0)
      expect(generated.stderr).toBe('')
      const contractBefore = readFileSync(join(outDir, 'contract.ts'), 'utf8')
      const schemaBefore = readFileSync(join(outDir, 'schema.ts'), 'utf8')

      const checked = runGenerator(['--contract', contractPath, '--out', outDir, '--check'])
      expect(checked.status).toBe(0)

      const regenerated = runGenerator(['--contract', contractPath, '--out', outDir])
      expect(regenerated.status).toBe(0)
      expect(readFileSync(join(outDir, 'contract.ts'), 'utf8')).toBe(contractBefore)
      expect(readFileSync(join(outDir, 'schema.ts'), 'utf8')).toBe(schemaBefore)
    },
    GENERATOR_TEST_TIMEOUT_MS,
  )

  it(
    'generates the enums and limits from the contract',
    () => {
      const outDir = temporaryDirectory()
      expect(runGenerator(['--contract', contractPath, '--out', outDir]).status).toBe(0)

      const document = JSON.parse(readFileSync(contractPath, 'utf8'))
      const schemas = document.components.schemas
      const errorCodeValues = schemas.ProblemDetail.properties.code.enum
      const spaceRoleValues = schemas.SpaceRole.enum
      const nameSchema = schemas.CreateSpaceRequest.properties.name
      const slugSchema = schemas.CreateSpaceRequest.properties.slug
      const bulkIdsSchema = schemas.BulkChecksRequest.properties.ids

      const enums = readFileSync(join(outDir, 'enums.ts'), 'utf8')
      const limits = readFileSync(join(outDir, 'limits.ts'), 'utf8')

      expect(enums).toContain('export const ErrorCode = {')
      expect(enums).toContain('  SLUG_TAKEN:')
      expect(enums).toContain('export const SpaceRole = {')
      expect(enums).toContain('export type SpaceRole = (typeof SpaceRole)[keyof typeof SpaceRole]')
      for (const value of [...errorCodeValues, ...spaceRoleValues]) {
        expect(enums).toContain(`'${value}'`)
      }
      expect(limits).toContain(
        `export const CREATE_SPACE_REQUEST_NAME_MAX_LENGTH = ${nameSchema.maxLength}`,
      )
      expect(limits).toContain(
        `export const CREATE_SPACE_REQUEST_SLUG_PATTERN = '${slugSchema.pattern}'`,
      )
      expect(limits).toContain(
        `export const BULK_CHECKS_REQUEST_IDS_MAX_ITEMS = ${bulkIdsSchema.maxItems}`,
      )
    },
    GENERATOR_TEST_TIMEOUT_MS,
  )

  it(
    'check mode fails when the generated files drift from the contract',
    () => {
      const outDir = temporaryDirectory()
      const contractDir = temporaryDirectory()
      expect(runGenerator(['--contract', contractPath, '--out', outDir]).status).toBe(0)

      const document = JSON.parse(readFileSync(contractPath, 'utf8'))
      document.components.schemas.MeResponse.properties.email = { type: 'string' }
      const driftedContract = join(contractDir, 'public.json')
      writeFileSync(driftedContract, JSON.stringify(document), 'utf8')

      const checked = runGenerator(['--contract', driftedContract, '--out', outDir, '--check'])

      expect(checked.status).toBe(1)
      expect(checked.stderr).toContain('run pnpm generate:api')
    },
    GENERATOR_TEST_TIMEOUT_MS,
  )
})
