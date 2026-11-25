import type { CheckDetailsData } from '../features/checks/details'

export const NOW = new Date('2026-11-25T12:00:00Z')

export const checkDetailsFixture: CheckDetailsData = {
  name: 'API płatności',
  target: 'https://payments.example/health',
  state: 'down',
  paused: false,
  lastCheckedAt: '2026-11-25T11:59:48Z',
  facts: {
    uptime24h: '96,1%',
    dataCoverage: '99,9%',
    interval: '300 s',
    timeout: '10 s',
    type: 'HTTP(S)',
  },
  configIssues: ['Serwer zwraca 503 od 14:02, a oczekiwany kod to 200.'],
  events: [
    {
      at: '2026-11-25T12:02:00Z',
      state: 'down',
      category: 'timeout',
      summary: 'Warszawa · HTTP 503',
    },
    {
      at: '2026-11-25T11:47:00Z',
      state: 'degraded',
      category: 'slow',
      summary: 'Frankfurt · 2400 ms przy progu 1500 ms',
    },
  ],
  certificate: {
    issuer: "R11 · Let's Encrypt",
    validTo: '2027-02-01',
    daysLeft: 68,
    fingerprint: 'AB:CD:EF:01',
  },
  changes: [
    {
      at: '2026-11-23T18:10:00Z',
      actor: 'Anna K.',
      source: 'FILE',
      summary: 'Interwał 60 s → 300 s',
    },
  ],
}

export const unknownCheckFixture: CheckDetailsData = {
  ...checkDetailsFixture,
  state: 'unknown',
  lastCheckedAt: '2026-11-25T11:55:00Z',
}
