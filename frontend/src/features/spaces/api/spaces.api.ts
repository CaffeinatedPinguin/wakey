import { contract } from '../../../shared/api/generated/contract'
import { serverClient } from '../../../shared/api/instances'
import { operationPath } from '../../../shared/api/operationPath'
import type { CreateSpaceRequest } from '../../../shared/api/generated/types'
import type { SpaceDetail, SpaceSummary } from '../spaceTypes'
import { spaceSchema, spacesPageSchema } from './spaces.schemas'

export interface SpacesPage {
  items: SpaceSummary[]
  nextCursor: string | null
}

function toSummary(space: { slug: string; name: string }): SpaceSummary {
  return { slug: space.slug, name: space.name }
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
  const response = await serverClient.get<unknown>(
    operationPath(contract.getSpace.path, { slug }),
    { signal },
  )
  return spaceSchema.parse(response.data)
}

export async function createSpace(values: CreateSpaceRequest): Promise<SpaceSummary> {
  const response = await serverClient.post<unknown>(contract.createSpace.path, values)
  return toSummary(spaceSchema.parse(response.data))
}
