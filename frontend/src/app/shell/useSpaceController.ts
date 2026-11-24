import { useDisclosure } from '@mantine/hooks'
import type { UseMutationResult, UseQueryResult } from '@tanstack/react-query'
import { useNavigate, useRouterState, useSearch } from '@tanstack/react-router'
import { useEffect } from 'react'
import type {
  CreateSpaceDialogProps,
  CreateSpaceInput,
  SpaceFormValues,
  SpaceSubmitError,
  SpaceSummary,
  SpaceSwitcherProps,
  SpacesStatus,
} from '../../features/spaces'
import {
  isSpaceSlugTakenError,
  spaceFromSearch,
  useCreateSpace,
  useSpaces,
} from '../../features/spaces'

export interface SpaceController {
  switcher: SpaceSwitcherProps
  dialog: CreateSpaceDialogProps
  spaces: readonly SpaceSummary[]
  isUnknown: boolean
}

interface ControllerInput {
  spaces: readonly SpaceSummary[]
  currentSlug: string
  status: SpacesStatus
  canCreate: boolean
  opened: boolean
  submitting: boolean
  submitError: SpaceSubmitError
  isUnknown: boolean
  onSelect: (slug: string) => void
  onCreate: () => void
  onRetry: () => void
  onSubmit: (values: SpaceFormValues) => void
  onClose: () => void
}

function spacesStatus(query: UseQueryResult<SpaceSummary[]>): SpacesStatus {
  if (query.isPending) {
    return 'loading'
  }
  if (query.isError) {
    return 'error'
  }
  return query.data.length === 0 ? 'empty' : 'ready'
}

function submitErrorOf(error: Error | null): SpaceSubmitError {
  if (error === null) {
    return null
  }
  return isSpaceSlugTakenError(error) ? 'slug_taken' : 'generic'
}

function isUnknownSpace(
  requestedSlug: string,
  spaces: readonly SpaceSummary[],
  status: SpacesStatus,
): boolean {
  if (status === 'empty') {
    return true
  }
  if (status !== 'ready') {
    return false
  }
  return !spaces.some((space) => space.slug === requestedSlug)
}

type SpaceNavigate = ReturnType<typeof useNavigate>

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

function toController(input: ControllerInput): SpaceController {
  return {
    switcher: {
      spaces: input.spaces,
      currentSlug: input.currentSlug,
      status: input.status,
      canCreate: input.canCreate,
      onSelect: input.onSelect,
      onCreate: input.onCreate,
      onRetry: input.onRetry,
    },
    dialog: {
      opened: input.opened,
      submitting: input.submitting,
      submitError: input.submitError,
      onSubmit: input.onSubmit,
      onClose: input.onClose,
    },
    spaces: input.spaces,
    isUnknown: input.isUnknown,
  }
}

export function useSpaceController(canCreate: boolean): SpaceController {
  const spacesQuery = useSpaces()
  const create = useCreateSpace()
  const search = useSearch({ strict: false })
  const pathname = useRouterState({ select: (state) => state.location.pathname })
  const navigate = useNavigate()
  const [opened, { open, close }] = useDisclosure(false)
  const spaces = spacesQuery.data ?? []
  const requestedSlug = spaceFromSearch(search)
  const firstSlug = spaces[0]?.slug
  const status = spacesStatus(spacesQuery)
  useDefaultSpace(navigate, pathname, requestedSlug, firstSlug)
  const selectSpace = (slug: string): void => {
    void navigate({ to: pathname, search: (previous) => ({ ...previous, space: slug }) })
  }
  const submit = submitSpace(create, close, selectSpace)
  return toController({
    spaces,
    currentSlug: requestedSlug ?? firstSlug ?? '',
    status,
    canCreate,
    opened,
    submitting: create.isPending,
    submitError: submitErrorOf(create.error),
    isUnknown: requestedSlug !== undefined && isUnknownSpace(requestedSlug, spaces, status),
    onSelect: selectSpace,
    onCreate: open,
    onRetry: () => {
      void spacesQuery.refetch()
    },
    onSubmit: submit,
    onClose: close,
  })
}
