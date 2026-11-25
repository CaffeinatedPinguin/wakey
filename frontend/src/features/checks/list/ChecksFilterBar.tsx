import { Button, Checkbox, Drawer, Group, Select, Stack, Text, TextInput } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { IconFilter } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { withFacetToggle } from './filterUtils'
import { sheetStyles, touchStyle } from './listStyles'
import type { FacetKey, FacetOption, FilterFacet, ListFilters } from './types'
import { useMobileList } from './useMobileList'

const sortValues = ['name', 'state', 'lastResult']

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
  facetKey: FacetKey
  option: FacetOption
  filters: ListFilters
  onFiltersChange: (filters: ListFilters) => void
}

function FacetOptionRow(props: FacetOptionRowProps): ReactElement {
  const { filters, facetKey, option, onFiltersChange } = props

  return (
    <Group justify="space-between" wrap="nowrap">
      <Checkbox
        label={option.label}
        checked={filters[facetKey].includes(option.value)}
        onChange={() => {
          onFiltersChange(withFacetToggle(filters, facetKey, option.value))
        }}
      />
      <Text size="xs" c="dimmed">
        {option.count}
      </Text>
    </Group>
  )
}

function FacetGroup({ facet, filters, onFiltersChange }: FacetGroupProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Stack gap="xs">
      <Text size="xs" fw={600} tt="uppercase" c="dimmed">
        {t(`checks.list.facets.${facet.key}`)}
      </Text>
      {facet.options.map((option) => (
        <FacetOptionRow
          key={option.value}
          facetKey={facet.key}
          option={option}
          filters={filters}
          onFiltersChange={onFiltersChange}
        />
      ))}
    </Stack>
  )
}

function FilterControls({ filters, facets, onFiltersChange }: FilterControlsProps): ReactElement {
  const { t } = useTranslation()
  const sortOptions = sortValues.map((value) => ({
    value,
    label: t(`checks.list.sort.${value}`),
  }))

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
      <Select
        label={t('checks.list.filters.sort')}
        data={sortOptions}
        value={filters.sort}
        allowDeselect={false}
        onChange={(value) => {
          onFiltersChange({ ...filters, sort: value ?? '' })
        }}
      />
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
  const isMobile = useMobileList()

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
