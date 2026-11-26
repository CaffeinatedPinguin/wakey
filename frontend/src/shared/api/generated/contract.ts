// This file is generated, do not edit.
// Source: api/openapi/public.json

export const contract = {
  addSpaceMember: { method: 'post', path: '/api/v1/spaces/{slug}/members' },
  changeSpaceMemberRole: { method: 'put', path: '/api/v1/spaces/{slug}/members/role' },
  createSpace: { method: 'post', path: '/api/v1/spaces' },
  getMe: { method: 'get', path: '/api/v1/me' },
  getSpace: { method: 'get', path: '/api/v1/spaces/{slug}' },
  listSpaceMembers: { method: 'get', path: '/api/v1/spaces/{slug}/members' },
  listSpaces: { method: 'get', path: '/api/v1/spaces' },
  putSpace: { method: 'put', path: '/api/v1/spaces/{slug}' },
  removeSpaceMember: { method: 'delete', path: '/api/v1/spaces/{slug}/members' },
} as const

export type OperationId = keyof typeof contract
