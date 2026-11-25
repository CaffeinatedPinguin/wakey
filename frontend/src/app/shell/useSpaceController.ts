import { useDisclosure } from '@mantine/hooks'
import type { UseMutationResult } from '@tanstack/react-query'
import { useNavigate, useRouterState, useSearch } from '@tanstack/react-router'
import { useEffect } from 'react'
import type {
  CreateSpaceDialogProps,
  CreateSpaceInput,
  SpaceFieldErrors,
  SpaceFormValues,
  SpaceSubmitError,
  SpaceSummary,
  SpaceSwitcherDataProps,
  SpacesStatus,
} from '../../features/spaces'
import {
  hasSpaceFieldErrors,
  isSlugTakenError,
  spaceFieldErrors,
  spaceFromSearch,
  useCreateSpace,
  useSpace,
  useSpaces,
} from '../../features/spaces'
import { isNotFoundError } from '../../shared/api/client'

export interface SpaceController {
  switcher: SpaceSwitcherDataProps
  dialog: CreateSpaceDialogProps
  spaces: readonly SpaceSummary[]
  isUnknown: boolean
}

type SpacesQuery = ReturnType<typeof useSpaces>

function flattenSpaces(query: SpacesQuery): SpaceSummary[] {
  return query.data?.pages.flatMap((page) => page.items) ?? []
}

function spacesStatus(query: SpacesQuery): SpacesStatus {
  if (query.isPending) {
    return 'loading'
  }
  if (query.isError) {
    return 'error'
  }
  return flattenSpaces(query).length === 0 ? 'empty' : 'ready'
}

function submitErrorOf(error: Error | null, fieldErrors: SpaceFieldErrors): SpaceSubmitError {
  if (hasSpaceFieldErrors(fieldErrors)) {
    return null
  }
  if (error === null) {
    return null
  }
  return isSlugTakenError(error) ? 'slug_taken' : 'generic'
}

function useSpacesState() {
  const query = useSpaces()
  const search = useSearch({ strict: false })
  const requestedSlug = spaceFromSearch(search)
  const detail = useSpace(requestedSlug)
  const spaces = flattenSpaces(query)
  return {
    query,
    spaces,
    status: spacesStatus(query),
    requestedSlug,
    detail,
    isUnknown: detail.isError && isNotFoundError(detail.error),
  }
}

type SpaceNavigate = ReturnType<typeof useNavigate>

function createSelectSpace(navigate: SpaceNavigate, pathname: string): (slug: string) => void {
  return (slug) => {
    void navigate({ to: pathname, search: (previous) => ({ ...previous, space: slug }) })
  }
}

function useDefaultSpace(
  navigate: SpaceNavigate,
  pathname: string,
  requestedSlug: string | undefined,
  firstSlug: string | undefined,
): void {
  useEffect(() => {
    if (requestedSlug !== undefined || firstSlug === undefined) {
      return
    }
    void navigate({
      to: pathname,
      search: (previous) => ({ ...previous, space: firstSlug }),
      replace: true,
    })
  }, [navigate, pathname, requestedSlug, firstSlug])
}

function submitSpace(
  create: UseMutationResult<SpaceSummary, Error, CreateSpaceInput>,
  close: () => void,
  selectSpace: (slug: string) => void,
): (values: SpaceFormValues) => void {
  return (values) => {
    create.mutate(values, {
      onSuccess: (space) => {
        close()
        selectSpace(space.slug)
      },
    })
  }
}

interface SwitcherInput {
  query: SpacesQuery
  spaces: readonly SpaceSummary[]
  status: SpacesStatus
  canCreate: boolean
  currentSlug: string
  currentName: string | undefined
  onSelect: (slug: string) => void
  onCreate: () => void
}

function switcherData(input: SwitcherInput): SpaceSwitcherDataProps {
  const { query, ...rest } = input
  return {
    ...rest,
    hasMore: query.hasNextPage,
    onLoadMore: () => {
      void query.fetchNextPage()
    },
    onRetry: () => {
      void query.refetch()
    },
  }
}

interface DialogInput {
  opened: boolean
  submitting: boolean
  error: Error | null
  fieldErrors: SpaceFieldErrors
  onSubmit: (values: SpaceFormValues) => void
  onClose: () => void
}

function dialogData(input: DialogInput): CreateSpaceDialogProps {
  return {
    opened: input.opened,
    submitting: input.submitting,
    submitError: submitErrorOf(input.error, input.fieldErrors),
    fieldErrors: input.fieldErrors,
    onSubmit: input.onSubmit,
    onClose: input.onClose,
  }
}

export function useSpaceController(canCreate: boolean): SpaceController {
  const { query, spaces, status, requestedSlug, detail, isUnknown } = useSpacesState()
  const create = useCreateSpace()
  const pathname = useRouterState({ select: (state) => state.location.pathname })
  const navigate = useNavigate()
  const [opened, { open, close }] = useDisclosure(false)
  const firstSlug = spaces[0]?.slug
  const currentSlug = requestedSlug ?? firstSlug ?? ''
  const currentName = spaces.find((space) => space.slug === currentSlug)?.name ?? detail.data?.name
  const fieldErrors = spaceFieldErrors(create.error)
  useDefaultSpace(navigate, pathname, requestedSlug, firstSlug)
  const selectSpace = createSelectSpace(navigate, pathname)
  const submit = submitSpace(create, close, selectSpace)
  return {
    switcher: switcherData({
      query,
      spaces,
      status,
      canCreate,
      currentSlug,
      currentName,
      onSelect: selectSpace,
      onCreate: open,
    }),
    dialog: dialogData({
      opened,
      submitting: create.isPending,
      error: create.error,
      fieldErrors,
      onSubmit: submit,
      onClose: () => {
        close()
        create.reset()
      },
    }),
    spaces,
    isUnknown,
  }
}
