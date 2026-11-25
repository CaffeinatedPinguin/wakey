// This file is generated, do not edit.
// Source: api/openapi/public.json

export const contract = {
  countChecks: { method: 'get', path: '/api/v1/spaces/{slug}/checks/counts' },
  createCheck: { method: 'post', path: '/api/v1/spaces/{slug}/checks' },
  createToken: { method: 'post', path: '/api/v1/spaces/{slug}/tokens' },
  deleteCheck: { method: 'delete', path: '/api/v1/spaces/{slug}/checks/{id}' },
  deleteSpaceSetting: { method: 'delete', path: '/api/v1/spaces/{slug}/settings/{key}' },
  getCheck: { method: 'get', path: '/api/v1/spaces/{slug}/checks/{id}' },
  getMe: { method: 'get', path: '/api/v1/me' },
  getSpace: { method: 'get', path: '/api/v1/spaces/{slug}' },
  getSpaceSettings: { method: 'get', path: '/api/v1/spaces/{slug}/settings' },
  listChecks: { method: 'get', path: '/api/v1/spaces/{slug}/checks' },
  listSpaces: { method: 'get', path: '/api/v1/spaces' },
  listTokens: { method: 'get', path: '/api/v1/spaces/{slug}/tokens' },
  pauseCheck: { method: 'post', path: '/api/v1/spaces/{slug}/checks/{id}/pause' },
  putSpace: { method: 'put', path: '/api/v1/spaces/{slug}' },
  putSpacePlan: { method: 'put', path: '/api/v1/spaces/{slug}/plan' },
  putSpaceSetting: { method: 'put', path: '/api/v1/spaces/{slug}/settings/{key}' },
  resumeCheck: { method: 'post', path: '/api/v1/spaces/{slug}/checks/{id}/resume' },
  revokeToken: { method: 'delete', path: '/api/v1/spaces/{slug}/tokens/{id}' },
  updateCheck: { method: 'put', path: '/api/v1/spaces/{slug}/checks/{id}' },
} as const

export type OperationId = keyof typeof contract
