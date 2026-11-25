import { act, cleanup, fireEvent, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { serverClient } from '../../shared/api/instances'
import { appConfig } from '../../shared/config'
import { meQueryKey } from './api/auth.queries'
import { signedInUser, unauthorizedBody } from '../../test/data'
import { installMockApi } from '../../test/mockApi'
import type { MockReply, MockRequest } from '../../test/mockApi'
import { renderApp } from '../../test/renderApp'

const ME_URL = `${appConfig.apiBaseUrl}/me`

let meStatus = 200
let meBody: unknown = signedInUser
let meRequestUrl: string | undefined
let logoutUrl: string | undefined
let logoutHeader: string | undefined

function handleRequest(request: MockRequest): MockReply {
  const method = request.method.toLowerCase()
  if (request.url === ME_URL) {
    meRequestUrl = request.url
    return { status: meStatus, data: meBody }
  }
  if (request.url === appConfig.logoutUrl && method === 'post') {
    logoutUrl = request.url
    logoutHeader = request.headers['x-xsrf-token']
    return { status: 204 }
  }
  return { status: 404, data: { code: 'not_found' } }
}

beforeEach(() => {
  meStatus = 200
  meBody = signedInUser
  meRequestUrl = undefined
  logoutUrl = undefined
  logoutHeader = undefined
  installMockApi(handleRequest)
})

afterEach(() => {
  cleanup()
  document.cookie = 'XSRF-TOKEN=; max-age=0; path=/'
})

describe('login screen', () => {
  it('shows the login screen when the session is missing', async () => {
    meStatus = 401
    meBody = unauthorizedBody
    renderApp('/')

    expect(await screen.findByRole('link', { name: 'Zaloguj się' })).toBeTruthy()
  })

  it('login button goes to the server login endpoint', async () => {
    renderApp('/login')
    const loginButton = await screen.findByRole('link', { name: 'Zaloguj się' })

    expect(loginButton.getAttribute('href')).toBe('/oauth2/authorization/oidc')
  })

  it('shows a generic message after a failed login', async () => {
    meStatus = 401
    meBody = unauthorizedBody
    renderApp('/login?login=failed')

    expect(await screen.findByText('Logowanie nie powiodło się. Spróbuj ponownie.')).toBeTruthy()
    expect(screen.queryByText(/401/)).toBeNull()
  })
})

describe('signed-in session', () => {
  it('shows the signed-in user and the admin badge', async () => {
    renderApp('/')

    expect(await screen.findByText(signedInUser.subject)).toBeTruthy()
    expect(screen.getByText('Admin')).toBeTruthy()
  })

  it('requests the session through the api client', async () => {
    renderApp('/')

    expect(await screen.findByText(signedInUser.subject)).toBeTruthy()
    expect(meRequestUrl).toBe(ME_URL)
  })

  it('logout posts with the CSRF header and shows the logged-out screen', async () => {
    document.cookie = 'XSRF-TOKEN=csrf-token-value; path=/'
    renderApp('/')

    fireEvent.click(await screen.findByRole('button', { name: 'Profil' }))
    fireEvent.click(await screen.findByText('Wyloguj'))

    expect(await screen.findByRole('heading', { name: 'Wylogowano' })).toBeTruthy()
    expect(logoutUrl).toBe(appConfig.logoutUrl)
    expect(logoutHeader).toBe('csrf-token-value')
  })

  it('an expired session returns to the login screen', async () => {
    const { queryClient } = renderApp('/')
    expect(await screen.findByText(signedInUser.subject)).toBeTruthy()

    meStatus = 401
    meBody = unauthorizedBody
    await act(async () => {
      await queryClient.refetchQueries({ queryKey: meQueryKey })
    })

    expect(await screen.findByRole('link', { name: 'Zaloguj się' })).toBeTruthy()
  })
})

describe('cross-origin requests', () => {
  it('does not send the CSRF header to another origin', async () => {
    document.cookie = 'XSRF-TOKEN=csrf-token-value; path=/'
    let foreignHeader: string | undefined
    installMockApi((request) => {
      foreignHeader = request.headers['x-xsrf-token']
      return { status: 204 }
    })

    await serverClient.post('https://other.example/logout')

    expect(foreignHeader).toBeUndefined()
  })
})
