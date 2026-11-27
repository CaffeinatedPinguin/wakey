import type { CheckDetailsData } from '../features/checks/details/checkDetailsTypes'

export const NOW = new Date('2026-11-25T12:00:00Z')

export const checkDetailsFixture: CheckDetailsData = {
  name: 'API płatności',
  target: 'https://payments.example/health',
  state: 'down',
  paused: false,
  lastCheckedAt: '2026-11-25T11:59:48Z',
  facts: {
    interval: '300 s',
    timeout: '10 s',
    type: 'HTTP(S)',
  },
}

export const unknownCheckFixture: CheckDetailsData = {
  ...checkDetailsFixture,
  state: 'unknown',
  lastCheckedAt: '2026-11-25T11:55:00Z',
}
