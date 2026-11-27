export interface MockRequest {
  method: string
  url: string
  headers: Record<string, string>
  body: string
}

export interface MockReply {
  status: number
  data?: unknown
  networkError?: boolean
  // A test can hold a response open to observe how pending requests overlap.
  waitFor?: Promise<void>
}

export type MockHandler = (request: MockRequest) => MockReply

let handler: MockHandler = () => ({ status: 404 })

// A minimal XMLHttpRequest so axios keeps using its real browser adapter,
// including the XSRF cookie handling. No request leaves the test.
class MockXMLHttpRequest {
  onloadend: (() => void) | null = null
  onreadystatechange: (() => void) | null = null
  onerror: ((event: { message?: string }) => void) | null = null
  readyState = 0
  status = 0
  statusText = ''
  responseText = ''
  responseURL = ''
  responseType = ''
  timeout = 0
  withCredentials = false
  private method = ''
  private url = ''
  private headers: Record<string, string> = {}
  private body = ''

  open(method: string, url: string): void {
    this.method = method
    this.url = url
    this.readyState = 1
  }

  setRequestHeader(name: string, value: string): void {
    this.headers[name.toLowerCase()] = value
  }

  getAllResponseHeaders(): string {
    return 'content-type: application/json'
  }

  // Axios aborts a request through this method when a query is cancelled, so a
  // no-op keeps the stub usable with callers that pass an AbortSignal.
  abort(): void {
    return undefined
  }

  send(body?: unknown): void {
    this.body = typeof body === 'string' ? body : ''
    const reply = handler({
      method: this.method,
      url: this.url,
      headers: { ...this.headers },
      body: this.body,
    })
    this.readyState = 4
    if (reply.networkError === true) {
      this.onerror?.({ message: 'Network Error' })
      return
    }
    this.status = reply.status
    this.responseText = reply.data === undefined ? '' : JSON.stringify(reply.data)
    this.responseURL = this.url
    const complete = (): void => {
      this.onloadend?.()
    }
    if (reply.waitFor === undefined) {
      complete()
      return
    }
    void reply.waitFor.then(complete)
  }
}

export function installMockApi(nextHandler: MockHandler): void {
  handler = nextHandler
  globalThis.XMLHttpRequest = MockXMLHttpRequest as unknown as typeof XMLHttpRequest
}
