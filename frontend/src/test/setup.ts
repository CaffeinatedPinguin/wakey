class ResizeObserverStub {
  observe(): void {
    return undefined
  }

  unobserve(): void {
    return undefined
  }

  disconnect(): void {
    return undefined
  }
}

window.matchMedia = (query: string): MediaQueryList => {
  const mediaQueryList = {
    matches: false,
    media: query,
    onchange: null,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => false,
  }
  return mediaQueryList as unknown as MediaQueryList
}

globalThis.ResizeObserver = ResizeObserverStub
