export interface MockRequest {
  method: string
  url: string
  headers: Record<string, string>
}

export interface MockReply {
  status: number
  data?: unknown
  networkError?: boolean
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

  send(): void {
    const reply = handler({
      method: this.method,
      url: this.url,
      headers: { ...this.headers },
    })
    this.readyState = 4
    if (reply.networkError === true) {
      this.onerror?.({ message: 'Network Error' })
      return
    }
    this.status = reply.status
    this.responseText = reply.data === undefined ? '' : JSON.stringify(reply.data)
    this.responseURL = this.url
    this.onloadend?.()
  }
}

export function installMockApi(nextHandler: MockHandler): void {
  handler = nextHandler
  globalThis.XMLHttpRequest = MockXMLHttpRequest as unknown as typeof XMLHttpRequest
}
