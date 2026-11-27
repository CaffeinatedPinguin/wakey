/** The HTTP statuses the client branches on; the server owns the rest. */
export const HttpStatus = {
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
} as const
export type HttpStatus = (typeof HttpStatus)[keyof typeof HttpStatus]
