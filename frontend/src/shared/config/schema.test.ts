import { describe, expect, it } from 'vitest'
import { appConfig } from './index'
import { parseAppConfig } from './schema'

const validEnv = {
  VITE_API_BASE_PATH: '/api/v1',
  VITE_LOGIN_PATH: '/oauth2/authorization/oidc',
  VITE_LOGOUT_PATH: '/logout',
}

describe('app configuration', () => {
  it('loads the committed defaults through Vite', () => {
    expect(appConfig).toEqual({
      apiBaseUrl: '/api/v1',
      loginUrl: '/oauth2/authorization/oidc',
      logoutUrl: '/logout',
    })
    expect(Object.isFrozen(appConfig)).toBe(true)
  })

  it('maps the environment to a frozen typed configuration', () => {
    const config = parseAppConfig(validEnv)

    expect(config).toEqual({
      apiBaseUrl: '/api/v1',
      loginUrl: '/oauth2/authorization/oidc',
      logoutUrl: '/logout',
    })
    expect(Object.isFrozen(config)).toBe(true)
  })

  it('rejects a missing address with a readable error', () => {
    const incomplete = {
      VITE_API_BASE_PATH: '/api/v1',
      VITE_LOGOUT_PATH: '/logout',
    }

    expect(() => parseAppConfig(incomplete)).toThrow(/VITE_LOGIN_PATH/)
  })

  it('rejects an address that is not a server path', () => {
    const relativePath = { ...validEnv, VITE_API_BASE_PATH: 'api/v1' }
    const foreignUrl = { ...validEnv, VITE_API_BASE_PATH: '//other.example' }

    expect(() => parseAppConfig(relativePath)).toThrow(/VITE_API_BASE_PATH/)
    expect(() => parseAppConfig(foreignUrl)).toThrow(/VITE_API_BASE_PATH/)
  })
})
