import { ApiError } from '../../../shared/api/problem'
import { contract } from '../../../shared/api/generated/contract'
import { serverClient } from '../../../shared/api/instances'
import type { SpaceFieldErrors, SpaceSummary } from '../spaceTypes'
import { spaceSchema, spacesPageSchema } from './spaces.schemas'

const SPACE_PAGE_LIMIT = 100
const SLUG_PLACEHOLDER = '{slug}'

const FIELD_BY_POINTER: Record<string, keyof SpaceFieldErrors | undefined> = {
  '/name': 'name',
  '/slug': 'slug',
}

export interface CreateSpaceInput {
  name: string
  slug: string
}

export interface SpacesPage {
  items: SpaceSummary[]
  nextCursor: string | null
}

// The server answers 409 with `code: slug_taken` when the new slug already exists.
export function isSlugTakenError(error: unknown): boolean {
  return error instanceof ApiError && error.code === 'slug_taken'
}

// A 400 `validation_failed` (or a top-level `invalid_slug`) carries `errors[]`
// with JSON Pointers, so the form can show the message on the right field.
export function spaceFieldErrors(error: unknown): SpaceFieldErrors {
  const fields: SpaceFieldErrors = { name: false, slug: false }
  if (!(error instanceof ApiError)) {
    return fields
  }
  if (error.code !== 'validation_failed' && error.code !== 'invalid_slug') {
    return fields
  }
  if (error.code === 'invalid_slug') {
    fields.slug = true
  }
  for (const fieldError of error.errors) {
    const field = FIELD_BY_POINTER[fieldError.pointer]
    if (field !== undefined) {
      fields[field] = true
    }
  }
  return fields
}

export function hasSpaceFieldErrors(errors: SpaceFieldErrors): boolean {
  return errors.name || errors.slug
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
  const response = await serverClient.post<unknown>(contract.createSpace.path, {
    slug: values.slug,
    name: values.name,
  })
  return toSummary(spaceSchema.parse(response.data))
}
