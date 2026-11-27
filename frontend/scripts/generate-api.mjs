// Generates the typed API surface from the OpenAPI document committed in the
// repository. The output is deterministic: fixed header, sorted operations and
// a final newline, so `--check` can compare it byte for byte.
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import openapiTS, { astToString } from 'openapi-typescript'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const defaultContract = join(scriptDir, '..', '..', 'api', 'openapi', 'public.json')
const defaultOutDir = join(scriptDir, '..', 'src', 'shared', 'api', 'generated')

const HEADER = '// This file is generated, do not edit.\n// Source: api/openapi/public.json\n\n'

const METHODS = ['get', 'put', 'post', 'delete', 'patch', 'head', 'options', 'trace']

function isHttpStatus(code) {
  return /^2\d\d$/.test(code)
}

function compareOperations(left, right) {
  return left.operationId < right.operationId ? -1 : left.operationId > right.operationId ? 1 : 0
}

function collectOperations(document) {
  const operations = []
  for (const [path, item] of Object.entries(document.paths ?? {})) {
    if (item === null || typeof item !== 'object') {
      continue
    }
    for (const method of METHODS) {
      const operation = item[method]
      if (operation === null || typeof operation !== 'object') {
        continue
      }
      if (typeof operation.operationId !== 'string') {
        continue
      }
      operations.push({ method, operation, operationId: operation.operationId, path })
    }
  }
  return operations.sort(compareOperations)
}

function pascalCase(value) {
  const words = value.split(/[^A-Za-z0-9]+/).filter((word) => word !== '')
  const joined = words.map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join('')
  return joined === '' ? 'Operation' : joined
}

function firstKey(value) {
  if (value === null || typeof value !== 'object') {
    return undefined
  }
  return Object.keys(value)[0]
}

function pickResponse(operation) {
  const responses = operation.responses
  if (responses === null || typeof responses !== 'object') {
    return undefined
  }
  const codes = Object.keys(responses).filter(isHttpStatus).sort()
  if (codes.length === 0) {
    return undefined
  }
  const code = codes.includes('200') ? '200' : codes[0]
  const mediaType = firstKey(responses[code]?.content)
  if (mediaType === undefined) {
    return undefined
  }
  return { code, mediaType }
}

function pickRequestMediaType(operation) {
  return firstKey(operation.requestBody?.content)
}

function parametersIn(operation, location) {
  const parameters = operation.parameters
  if (!Array.isArray(parameters)) {
    return false
  }
  return parameters.some((parameter) => parameter?.in === location)
}

function operationLines({ operation, operationId }) {
  const name = pascalCase(operationId)
  const lines = []
  const response = pickResponse(operation)
  if (response !== undefined) {
    lines.push(
      `export type ${name}Response = operations['${operationId}']['responses'][${response.code}]['content']['${response.mediaType}']`,
    )
  }
  const requestMediaType = pickRequestMediaType(operation)
  if (requestMediaType !== undefined) {
    lines.push(
      `export type ${name}Request = operations['${operationId}']['requestBody']['content']['${requestMediaType}']`,
    )
  }
  if (parametersIn(operation, 'query')) {
    lines.push(
      `export type ${name}Query = NonNullable<operations['${operationId}']['parameters']['query']>`,
    )
  }
  if (parametersIn(operation, 'path')) {
    lines.push(
      `export type ${name}Path = NonNullable<operations['${operationId}']['parameters']['path']>`,
    )
  }
  return lines
}

function generateTypes(document, operations, hasProblemDetail) {
  const lines = [HEADER + "import type { components, operations } from './schema'", '']
  if (hasProblemDetail) {
    lines.push("export type ProblemDetail = components['schemas']['ProblemDetail']")
  }
  for (const operation of operations) {
    lines.push(...operationLines(operation))
  }
  const aliases = lines.join('\n')
  return `${aliases.replace(/\n+$/, '')}\n`
}

function generateContract(operations) {
  const entries = operations.map(
    ({ method, operationId, path }) =>
      `  ${operationId}: { method: '${method}', path: '${path}' },`,
  )
  const body = [
    'export const contract = {',
    ...entries,
    '} as const',
    '',
    'export type OperationId = keyof typeof contract',
  ]
  return `${HEADER}${body.join('\n')}\n`
}

async function generateSchema(document) {
  const ast = await openapiTS(document)
  const printed = astToString(ast)
  return `${HEADER}${printed.replace(/\s+$/, '')}\n`
}

function upperSnake(value) {
  return value
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/[^A-Za-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .toUpperCase()
}

function quote(value) {
  return `'${value.replaceAll('\\', '\\\\').replaceAll("'", "\\'")}'`
}

function stringEnumValues(schema) {
  if (schema === null || typeof schema !== 'object' || !Array.isArray(schema.enum)) {
    return undefined
  }
  const types = Array.isArray(schema.type) ? schema.type : [schema.type]
  if (!types.includes('string') || !schema.enum.every((value) => typeof value === 'string')) {
    return undefined
  }
  return [...schema.enum].sort()
}

function problemCodeValues(document) {
  const schemas = document.components?.schemas ?? {}
  return stringEnumValues(schemas.ProblemDetail?.properties?.code)
}

function generateEnums(document) {
  const schemas = document.components?.schemas ?? {}
  const enums = []
  const problemCodes = problemCodeValues(document)
  if (problemCodes !== undefined) {
    enums.push({ name: 'ErrorCode', values: problemCodes })
  }
  for (const [name, schema] of Object.entries(schemas)) {
    const values = stringEnumValues(schema)
    if (values !== undefined) {
      enums.push({ name, values })
    }
  }
  enums.sort((left, right) => (left.name < right.name ? -1 : left.name > right.name ? 1 : 0))
  const blocks = enums.map(({ name, values }) =>
    [
      `export const ${name} = {`,
      ...values.map((value) => `  ${upperSnake(value)}: ${quote(value)},`),
      '} as const',
      '',
      `export type ${name} = (typeof ${name})[keyof typeof ${name}]`,
    ].join('\n'),
  )
  return `${HEADER}${blocks.join('\n\n')}\n`
}

function requestSchemaNames(operations) {
  const names = new Set()
  for (const { operation } of operations) {
    const content = operation.requestBody?.content
    if (content === null || typeof content !== 'object') {
      continue
    }
    for (const media of Object.values(content)) {
      const reference = media?.schema?.$ref
      if (typeof reference === 'string') {
        names.add(reference.slice(reference.lastIndexOf('/') + 1))
      }
    }
  }
  return names
}

function propertyLimits(prefix, schema) {
  if (schema === null || typeof schema !== 'object') {
    return []
  }
  const entries = []
  if (typeof schema.minLength === 'number') {
    entries.push({ name: `${prefix}_MIN_LENGTH`, value: String(schema.minLength) })
  }
  if (typeof schema.maxLength === 'number') {
    entries.push({ name: `${prefix}_MAX_LENGTH`, value: String(schema.maxLength) })
  }
  if (typeof schema.maxItems === 'number') {
    entries.push({ name: `${prefix}_MAX_ITEMS`, value: String(schema.maxItems) })
  }
  if (typeof schema.pattern === 'string') {
    entries.push({ name: `${prefix}_PATTERN`, value: quote(schema.pattern) })
  }
  return entries
}

function requestLimitEntries(document, operations) {
  const schemas = document.components?.schemas ?? {}
  const entries = []
  for (const schemaName of [...requestSchemaNames(operations)].sort()) {
    const properties = schemas[schemaName]?.properties
    if (properties === null || typeof properties !== 'object') {
      continue
    }
    for (const propertyName of Object.keys(properties).sort()) {
      const prefix = `${upperSnake(schemaName)}_${upperSnake(propertyName)}`
      entries.push(...propertyLimits(prefix, properties[propertyName]))
    }
  }
  return entries
}

function queryLimitEntries(operations) {
  const entries = []
  for (const { operation, operationId } of operations) {
    if (!Array.isArray(operation.parameters)) {
      continue
    }
    for (const parameter of operation.parameters) {
      if (parameter?.in !== 'query' || typeof parameter.name !== 'string') {
        continue
      }
      const prefix = `${upperSnake(operationId)}_QUERY_${upperSnake(parameter.name)}`
      entries.push(...propertyLimits(prefix, parameter.schema))
    }
  }
  return entries
}

function limitEntries(document, operations) {
  const entries = [...requestLimitEntries(document, operations), ...queryLimitEntries(operations)]
  return entries.sort((left, right) =>
    left.name < right.name ? -1 : left.name > right.name ? 1 : 0,
  )
}

function generateLimits(document, operations) {
  const lines = limitEntries(document, operations).map(
    ({ name, value }) => `export const ${name} = ${value}`,
  )
  if (lines.length === 0) {
    return HEADER
  }
  return `${HEADER}${lines.join('\n')}\n`
}

async function generateAll(contractPath) {
  const raw = await readFile(contractPath, 'utf8')
  const document = JSON.parse(raw)
  const operations = collectOperations(document)
  const hasProblemDetail = document.components?.schemas?.ProblemDetail !== undefined
  return {
    'contract.ts': generateContract(operations),
    'enums.ts': generateEnums(document),
    'limits.ts': generateLimits(document, operations),
    'schema.ts': await generateSchema(document),
    'types.ts': generateTypes(document, operations, hasProblemDetail),
  }
}

function normalize(text) {
  return text.replace(/\r\n/g, '\n')
}

function parseArguments(argv) {
  const options = { check: false, contract: defaultContract, outDir: defaultOutDir }
  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index]
    if (argument === '--check') {
      options.check = true
    } else if (argument === '--contract') {
      index += 1
      options.contract = argv[index]
    } else if (argument === '--out') {
      index += 1
      options.outDir = argv[index]
    } else if (argument.startsWith('--contract=')) {
      options.contract = argument.slice('--contract='.length)
    } else if (argument.startsWith('--out=')) {
      options.outDir = argument.slice('--out='.length)
    } else {
      throw new Error(`Unknown argument: ${argument}`)
    }
  }
  return options
}

async function checkFiles(outDir, files) {
  const stale = []
  for (const [name, content] of Object.entries(files)) {
    let existing
    try {
      existing = await readFile(join(outDir, name), 'utf8')
    } catch {
      stale.push(name)
      continue
    }
    if (normalize(existing) !== normalize(content)) {
      stale.push(name)
    }
  }
  return stale
}

async function writeFiles(outDir, files) {
  await mkdir(outDir, { recursive: true })
  for (const [name, content] of Object.entries(files)) {
    await writeFile(join(outDir, name), content, 'utf8')
  }
}

async function main() {
  const options = parseArguments(process.argv.slice(2))
  const files = await generateAll(options.contract)
  if (options.check) {
    const stale = await checkFiles(options.outDir, files)
    if (stale.length > 0) {
      throw new Error(
        `Generated API files are out of date (${stale.join(', ')}). run pnpm generate:api.`,
      )
    }
    return
  }
  await writeFiles(options.outDir, files)
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error))
  process.exitCode = 1
})
