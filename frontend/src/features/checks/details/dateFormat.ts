const MILLIS_PER_MINUTE = 60_000
const MINUTES_PER_HOUR = 60
const HOURS_PER_DAY = 24

/** Relative age of a past timestamp, e.g. "5 minut temu" / "5 minutes ago". */
export function formatRelativeAge(at: string, now: Date, locale: string): string {
  const formatter = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' })
  const minutes = Math.round((now.getTime() - new Date(at).getTime()) / MILLIS_PER_MINUTE)
  if (Math.abs(minutes) < MINUTES_PER_HOUR) {
    return formatter.format(-minutes, 'minute')
  }
  const hours = Math.round(minutes / MINUTES_PER_HOUR)
  if (Math.abs(hours) < HOURS_PER_DAY) {
    return formatter.format(-hours, 'hour')
  }
  return formatter.format(-Math.round(hours / HOURS_PER_DAY), 'day')
}

/** Clock time of an event, e.g. "14:02". */
export function formatClock(at: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit' }).format(
    new Date(at),
  )
}

/** Date and time of a configuration change. */
export function formatMoment(at: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, { dateStyle: 'short', timeStyle: 'short' }).format(
    new Date(at),
  )
}
