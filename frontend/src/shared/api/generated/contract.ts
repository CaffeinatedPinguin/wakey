// This file is generated, do not edit.
// Source: api/openapi/public.json

export const contract = {
  createSpace: { method: 'post', path: '/api/v1/spaces' },
  getMe: { method: 'get', path: '/api/v1/me' },
  getSpace: { method: 'get', path: '/api/v1/spaces/{slug}' },
  listSpaces: { method: 'get', path: '/api/v1/spaces' },
  putSpace: { method: 'put', path: '/api/v1/spaces/{slug}' },
} as const

export type OperationId = keyof typeof contract
