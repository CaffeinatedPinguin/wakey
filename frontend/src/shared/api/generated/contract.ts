// This file is generated, do not edit.
// Source: api/openapi/public.json

export const contract = {
  countChecks: { method: 'get', path: '/api/v1/spaces/{slug}/checks/counts' },
  createCheck: { method: 'post', path: '/api/v1/spaces/{slug}/checks' },
  deleteCheck: { method: 'delete', path: '/api/v1/spaces/{slug}/checks/{id}' },
  getCheck: { method: 'get', path: '/api/v1/spaces/{slug}/checks/{id}' },
  getMe: { method: 'get', path: '/api/v1/me' },
  getSpace: { method: 'get', path: '/api/v1/spaces/{slug}' },
  listChecks: { method: 'get', path: '/api/v1/spaces/{slug}/checks' },
  listSpaces: { method: 'get', path: '/api/v1/spaces' },
  pauseCheck: { method: 'post', path: '/api/v1/spaces/{slug}/checks/{id}/pause' },
  putSpace: { method: 'put', path: '/api/v1/spaces/{slug}' },
  resumeCheck: { method: 'post', path: '/api/v1/spaces/{slug}/checks/{id}/resume' },
  updateCheck: { method: 'put', path: '/api/v1/spaces/{slug}/checks/{id}' },
} as const

export type OperationId = keyof typeof contract
