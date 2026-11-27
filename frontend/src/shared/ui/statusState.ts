/** The states a check or a status page item can be in; each has a Badge colour, an icon and a text. */
export const StatusState = {
  UP: 'up',
  DEGRADED: 'degraded',
  DOWN: 'down',
  UNKNOWN: 'unknown',
  MAINTENANCE: 'maintenance',
} as const
export type StatusState = (typeof StatusState)[keyof typeof StatusState]
