import { Button, Checkbox, Drawer, Group, Stack, Text, TextInput } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { IconFilter } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { useIsMobile } from '../../../shared/ui'
import { isFacetChosen, withFacetChoice } from './filterUtils'
import { sheetStyles, touchStyle } from './listStyles'
import { FacetKey } from './types'
import type { FacetOption, FilterFacet, ListFilters } from './types'

interface FilterControlsProps {
  filters: ListFilters
  facets: FilterFacet[]
  onFiltersChange: (filters: ListFilters) => void
}

interface ChecksFilterBarProps extends FilterControlsProps {
  totalCount: number
}

interface FacetGroupProps {
  facet: FilterFacet
  filters: ListFilters
  onFiltersChange: (filters: ListFilters) => void
}

interface FacetOptionRowProps {
  facet: FilterFacet
  option: FacetOption
  filters: ListFilters
  onFiltersChange: (filters: ListFilters) => void
}

function FacetOptionRow(props: FacetOptionRowProps): ReactElement {
  const { filters, facet, option, onFiltersChange } = props

  return (
    <Group justify="space-between" wrap="nowrap">
      <Checkbox
        label={option.label}
        checked={isFacetChosen(filters, facet, option.value)}
        onChange={() => {
          onFiltersChange(withFacetChoice(filters, facet, option.value))
        }}
      />
      <Text size="xs" c="dimmed">
        {option.count}
      </Text>
    </Group>
  )
}

interface FacetOptionsProps<Value extends string> {
  facet: FilterFacet
  options: FacetOption<Value>[]
  filters: ListFilters
  onFiltersChange: (filters: ListFilters) => void
}

function FacetOptions<Value extends string>(props: FacetOptionsProps<Value>): ReactElement {
  const { facet, options, filters, onFiltersChange } = props
  return (
    <>
      {options.map((option) => (
        <FacetOptionRow
          key={option.value}
          facet={facet}
          option={option}
          filters={filters}
          onFiltersChange={onFiltersChange}
        />
      ))}
    </>
  )
}

// Each contract facet keeps its own value type, so the chosen option stays a
// contract value instead of a plain string.
function FacetRows({ facet, filters, onFiltersChange }: FacetGroupProps): ReactElement {
  switch (facet.key) {
    case FacetKey.STATE:
      return (
        <FacetOptions
          facet={facet}
          options={facet.options}
          filters={filters}
          onFiltersChange={onFiltersChange}
        />
      )
    case FacetKey.TYPE:
      return (
        <FacetOptions
          facet={facet}
          options={facet.options}
          filters={filters}
          onFiltersChange={onFiltersChange}
        />
      )
    case FacetKey.TAG:
      return (
        <FacetOptions
          facet={facet}
          options={facet.options}
          filters={filters}
          onFiltersChange={onFiltersChange}
        />
      )
    case FacetKey.GROUP:
      return (
        <FacetOptions
          facet={facet}
          options={facet.options}
          filters={filters}
          onFiltersChange={onFiltersChange}
        />
      )
  }
}

function FacetGroup({ facet, filters, onFiltersChange }: FacetGroupProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Stack gap="xs">
      <Text size="xs" fw={600} tt="uppercase" c="dimmed">
        {t(`checks.list.facets.${facet.key}`)}
      </Text>
      <FacetRows facet={facet} filters={filters} onFiltersChange={onFiltersChange} />
    </Stack>
  )
}

function FilterControls({ filters, facets, onFiltersChange }: FilterControlsProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Stack gap="md">
      <TextInput
        label={t('checks.list.filters.search')}
        placeholder={t('checks.list.filters.searchPlaceholder')}
        value={filters.q}
        onChange={(event) => {
          onFiltersChange({ ...filters, q: event.currentTarget.value })
        }}
      />
      {facets.map((facet) => (
        <FacetGroup
          key={facet.key}
          facet={facet}
          filters={filters}
          onFiltersChange={onFiltersChange}
        />
      ))}
    </Stack>
  )
}

function MobileFilterSheet(props: ChecksFilterBarProps): ReactElement {
  const { t } = useTranslation()
  const [opened, { open, close }] = useDisclosure(false)

  return (
    <>
      <Button
        variant="default"
        leftSection={<IconFilter size="var(--app-size-icon)" />}
        style={touchStyle}
        onClick={open}
      >
        {t('checks.list.filters.trigger')}
      </Button>
      <Drawer
        opened={opened}
        onClose={close}
        position="bottom"
        size="auto"
        title={t('checks.list.filters.title')}
        styles={sheetStyles}
      >
        <Stack gap="md">
          <FilterControls
            filters={props.filters}
            facets={props.facets}
            onFiltersChange={props.onFiltersChange}
          />
          <Button fullWidth style={touchStyle} onClick={close}>
            {t('checks.list.filters.apply', { count: props.totalCount })}
          </Button>
        </Stack>
      </Drawer>
    </>
  )
}

export function ChecksFilterBar(props: ChecksFilterBarProps): ReactElement {
  const isMobile = useIsMobile()

  if (!isMobile) {
    return (
      <FilterControls
        filters={props.filters}
        facets={props.facets}
        onFiltersChange={props.onFiltersChange}
      />
    )
  }
  return <MobileFilterSheet {...props} />
}
