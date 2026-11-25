import { ApiError } from '../../../shared/api/problem'
import { contract } from '../../../shared/api/generated/contract'
import { serverClient } from '../../../shared/api/instances'
import type { SpaceSummary } from '../spaceTypes'
import { spaceSchema, spacesPageSchema } from './spaces.schemas'

const SPACE_PAGE_LIMIT = 100
const SLUG_PLACEHOLDER = '{slug}'

export interface CreateSpaceInput {
  name: string
  slug: string
}

export interface SpacesPage {
  items: SpaceSummary[]
  nextCursor: string | null
}

// The server answers 409 with `code: slug_taken` when If-None-Match: * names a
// slug that already exists.
export function isSlugTakenError(error: unknown): boolean {
  return error instanceof ApiError && error.code === 'slug_taken'
}

function spaceUrl(path: string, slug: string): string {
  return path.replace(SLUG_PLACEHOLDER, encodeURIComponent(slug))
}

function toSummary(space: { slug: string; name: string }): SpaceSummary {
  return { slug: space.slug, name: space.name }
}

export async function fetchSpaces(
  cursor: string | undefined,
  signal: AbortSignal,
): Promise<SpacesPage> {
  const response = await serverClient.get<unknown>(contract.listSpaces.path, {
    params: { limit: SPACE_PAGE_LIMIT, cursor },
    signal,
  })
  const page = spacesPageSchema.parse(response.data)
  return { items: page.items.map(toSummary), nextCursor: page.nextCursor }
}

export async function fetchSpace(slug: string, signal: AbortSignal): Promise<SpaceSummary> {
  const response = await serverClient.get<unknown>(spaceUrl(contract.getSpace.path, slug), {
    signal,
  })
  return toSummary(spaceSchema.parse(response.data))
}

export async function createSpace(values: CreateSpaceInput): Promise<SpaceSummary> {
  const url = spaceUrl(contract.putSpace.path, values.slug)
  const response = await serverClient.put<unknown>(
    url,
    { name: values.name },
    { headers: { 'If-None-Match': '*' } },
  )
  return toSummary(spaceSchema.parse(response.data))
}
