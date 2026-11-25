import type {
  CountChecksResponse as StateCountsResponse,
  GetCheckResponse as CheckResponse,
} from '../shared/api/generated/types'
import { appConfig } from '../shared/config'
import { signedInUser } from './data'
import type { MockHandler, MockReply, MockRequest } from './mockApi'

const BASE = appConfig.apiBaseUrl
const ME_URL = `${BASE}/me`
const SPACES_URL = `${BASE}/spaces`
const SPACES_PREFIX = `${SPACES_URL}/`
const DEFAULT_PAGE_SIZE = 50

export interface SpaceFixture {
  slug: string
  name: string
  admin: boolean
  role: 'EDITOR' | 'VIEWER' | null
  version: number
}

export const alphaSpace: SpaceFixture = {
  slug: 'alpha',
  name: 'Alpha',
  admin: true,
  role: null,
  version: 1,
}

export const betaSpace: SpaceFixture = {
  slug: 'beta',
  name: 'Beta',
  admin: true,
  role: null,
  version: 1,
}

const CHECK_DEFAULTS: CheckResponse = {
  id: 'c1',
  name: 'API płatności',
  type: 'HTTP',
  target: 'https://payments.example/health',
  intervalSeconds: 60,
  timeoutSeconds: 10,
  retries: 1,
  paused: false,
  tags: ['produkcja'],
  state: 'DOWN',
  version: 1,
  createdAt: '2026-11-20T10:00:00Z',
  updatedAt: '2026-11-25T10:00:00Z',
}

export function makeCheck(overrides: Partial<CheckResponse>): CheckResponse {
  return { ...CHECK_DEFAULTS, ...overrides }
}

export interface ChecksApiState {
  spaces: SpaceFixture[]
  checks: CheckResponse[]
  counts: StateCountsResponse
  pageSize: number
  listCalls: string[]
  countsCalls: string[]
  pauseCalls: string[]
  resumeCalls: string[]
  deleteCalls: string[]
  failPause: MockReply | null
}

export interface ChecksApiMock {
  state: ChecksApiState
  handler: MockHandler
}

function createState(): ChecksApiState {
  return {
    spaces: [alphaSpace],
    checks: [],
    counts: { up: 0, degraded: 0, down: 0, unknown: 0 },
    pageSize: DEFAULT_PAGE_SIZE,
    listCalls: [],
    countsCalls: [],
    pauseCalls: [],
    resumeCalls: [],
    deleteCalls: [],
    failPause: null,
  }
}

function listReply(state: ChecksApiState, url: URL): MockReply {
  state.listCalls.push(url.pathname + url.search)
  const rawCursor = url.searchParams.get('cursor')
  const start = rawCursor === null ? 0 : Number.parseInt(rawCursor, 10)
  const items = state.checks.slice(start, start + state.pageSize)
  const end = start + state.pageSize
  const nextCursor = end < state.checks.length ? String(end) : null
  return { status: 200, data: { items, nextCursor } }
}

function findCheck(state: ChecksApiState, id: string): CheckResponse | undefined {
  return state.checks.find((check) => check.id === id)
}

function mutateCheck(state: ChecksApiState, id: string, paused: boolean): MockReply {
  const check = findCheck(state, id)
  if (check === undefined) {
    return { status: 404, data: { code: 'not_found' } }
  }
  check.paused = paused
  if (paused) {
    check.pausedReason = 'MANUAL'
  } else {
    delete check.pausedReason
  }
  return { status: 200, data: check }
}

function handleCheckItem(
  state: ChecksApiState,
  id: string,
  action: string | undefined,
  method: string,
): MockReply {
  if (action === 'pause') {
    state.pauseCalls.push(id)
    return state.failPause ?? mutateCheck(state, id, true)
  }
  if (action === 'resume') {
    state.resumeCalls.push(id)
    return mutateCheck(state, id, false)
  }
  if (action === undefined && method === 'delete') {
    state.deleteCalls.push(id)
    state.checks = state.checks.filter((check) => check.id !== id)
    return { status: 204 }
  }
  const check = findCheck(state, id)
  return check === undefined
    ? { status: 404, data: { code: 'not_found' } }
    : { status: 200, data: check }
}

interface CheckRequest {
  slug: string
  tail: string[]
  method: string
  url: URL
}

function handleChecks(state: ChecksApiState, request: CheckRequest): MockReply {
  const checksPath = `${SPACES_PREFIX}${request.slug}/checks`
  if (request.url.pathname === checksPath && request.method === 'get') {
    return listReply(state, request.url)
  }
  if (request.url.pathname === `${checksPath}/counts`) {
    state.countsCalls.push(request.url.pathname + request.url.search)
    return { status: 200, data: state.counts }
  }
  const id = request.tail[1]
  if (id === undefined) {
    return { status: 404, data: { code: 'not_found' } }
  }
  return handleCheckItem(state, decodeURIComponent(id), request.tail[2], request.method)
}

function handleSpaces(state: ChecksApiState, tail: string[]): MockReply {
  const space = state.spaces.find((item) => item.slug === decodeURIComponent(tail[0] ?? ''))
  return space === undefined
    ? { status: 404, data: { code: 'not_found' } }
    : { status: 200, data: space }
}

function handleRequest(state: ChecksApiState, request: MockRequest): MockReply {
  const method = request.method.toLowerCase()
  const url = new URL(request.url, 'http://localhost')
  if (url.pathname === ME_URL) {
    return { status: 200, data: signedInUser }
  }
  if (url.pathname === SPACES_URL) {
    return { status: 200, data: { items: state.spaces, nextCursor: null } }
  }
  if (!url.pathname.startsWith(SPACES_PREFIX)) {
    return { status: 404, data: { code: 'not_found' } }
  }
  const rest = url.pathname.slice(SPACES_PREFIX.length).split('/')
  const slug = decodeURIComponent(rest[0] ?? '')
  const tail = rest.slice(1)
  if (tail[0] === 'checks') {
    return handleChecks(state, { slug, tail, method, url })
  }
  return handleSpaces(state, rest)
}

export function createChecksApiMock(): ChecksApiMock {
  const state = createState()
  return {
    state,
    handler: (request) => handleRequest(state, request),
  }
}

export function countsFor(checks: CheckResponse[]): StateCountsResponse {
  const counts: StateCountsResponse = { up: 0, degraded: 0, down: 0, unknown: 0 }
  for (const check of checks) {
    switch (check.state) {
      case 'UP':
        counts.up += 1
        break
      case 'DEGRADED':
        counts.degraded += 1
        break
      case 'DOWN':
        counts.down += 1
        break
      case 'UNKNOWN':
        counts.unknown += 1
        break
    }
  }
  return counts
}
