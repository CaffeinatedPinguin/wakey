interface RelativeUnit {
  readonly unit: Intl.RelativeTimeFormatUnit
  readonly seconds: number
}

const RELATIVE_UNITS: readonly RelativeUnit[] = [
  { unit: 'day', seconds: 86400 },
  { unit: 'hour', seconds: 3600 },
  { unit: 'minute', seconds: 60 },
]

const ABSOLUTE_THRESHOLD_SECONDS = 7 * 86400

function relativeFormatter(locale: string): Intl.RelativeTimeFormat {
  return new Intl.RelativeTimeFormat(locale, { numeric: 'auto' })
}

function formatAbsolute(timestamp: number, locale: string): string {
  return new Intl.DateTimeFormat(locale, { dateStyle: 'short' }).format(timestamp)
}

export function formatLastActive(iso: string, now: Date, locale: string): string {
  const timestamp = Date.parse(iso)
  if (Number.isNaN(timestamp)) {
    return '—'
  }

  const deltaSeconds = Math.round((timestamp - now.getTime()) / 1000)
  const absoluteSeconds = Math.abs(deltaSeconds)
  if (absoluteSeconds >= ABSOLUTE_THRESHOLD_SECONDS) {
    return formatAbsolute(timestamp, locale)
  }

  const selected = RELATIVE_UNITS.find((entry) => absoluteSeconds >= entry.seconds)
  if (selected === undefined) {
    return relativeFormatter(locale).format(0, 'second')
  }
  const value = Math.round(deltaSeconds / selected.seconds)
  return relativeFormatter(locale).format(value, selected.unit)
}
