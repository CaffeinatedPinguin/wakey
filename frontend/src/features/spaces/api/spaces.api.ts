import { isNotFoundError } from '../../../shared/api/client'
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

// A 200 from `getSpace` means the slug already exists. PUT is create-or-rename,
// so sending it would overwrite the existing space's name instead of failing.
export class SpaceSlugTakenError extends Error {
  constructor() {
    super('space slug is already taken')
    this.name = 'SpaceSlugTakenError'
  }
}

export function isSpaceSlugTakenError(error: unknown): boolean {
  return error instanceof SpaceSlugTakenError
}

function spaceUrl(path: string, slug: string): string {
  return path.replace(SLUG_PLACEHOLDER, encodeURIComponent(slug))
}

function toSummary(space: { slug: string; name: string }): SpaceSummary {
  return { slug: space.slug, name: space.name }
}

export async function fetchSpaces(signal: AbortSignal): Promise<SpaceSummary[]> {
  const response = await serverClient.get<unknown>(contract.listSpaces.path, {
    params: { limit: SPACE_PAGE_LIMIT },
    signal,
  })
  const page = spacesPageSchema.parse(response.data)
  return page.items.map(toSummary)
}

async function spaceExists(slug: string): Promise<boolean> {
  try {
    await serverClient.get<unknown>(spaceUrl(contract.getSpace.path, slug))
    return true
  } catch (error) {
    if (isNotFoundError(error)) {
      return false
    }
    throw error
  }
}

export async function createSpace(values: CreateSpaceInput): Promise<SpaceSummary> {
  const exists = await spaceExists(values.slug)
  if (exists) {
    throw new SpaceSlugTakenError()
  }
  const url = spaceUrl(contract.putSpace.path, values.slug)
  const response = await serverClient.put<unknown>(url, { name: values.name })
  return toSummary(spaceSchema.parse(response.data))
}
