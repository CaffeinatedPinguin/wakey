import { contract } from '../../../shared/api/generated/contract'
import { serverClient } from '../../../shared/api/instances'
import type { SpaceDetail, SpaceSummary } from '../spaceTypes'
import { spaceSchema, spacesPageSchema } from './spaces.schemas'

const SLUG_PLACEHOLDER = '{slug}'

export interface CreateSpaceInput {
  name: string
  slug: string
}

export interface SpacesPage {
  items: SpaceSummary[]
  nextCursor: string | null
}

function spaceUrl(path: string, slug: string): string {
  return path.replace(SLUG_PLACEHOLDER, encodeURIComponent(slug))
}

function toSummary(space: { slug: string; name: string }): SpaceSummary {
  return { slug: space.slug, name: space.name }
}

function toDetail(space: SpaceDetail): SpaceDetail {
  return { slug: space.slug, name: space.name, role: space.role }
}

export async function fetchSpaces(
  cursor: string | undefined,
  signal: AbortSignal,
): Promise<SpacesPage> {
  const response = await serverClient.get<unknown>(contract.listSpaces.path, {
    params: { cursor },
    signal,
  })
  const page = spacesPageSchema.parse(response.data)
  return { items: page.items.map(toSummary), nextCursor: page.nextCursor }
}

export async function fetchSpace(slug: string, signal: AbortSignal): Promise<SpaceDetail> {
  const response = await serverClient.get<unknown>(spaceUrl(contract.getSpace.path, slug), {
    signal,
  })
  return toDetail(spaceSchema.parse(response.data))
}

export async function createSpace(values: CreateSpaceInput): Promise<SpaceSummary> {
  const response = await serverClient.post<unknown>(contract.createSpace.path, {
    slug: values.slug,
    name: values.name,
  })
  return toSummary(spaceSchema.parse(response.data))
}
