const EM_TO_PIXELS = 16
const UNIT_LENGTH = 2

interface MediaFeature {
  atLeast: boolean
  pixels: number
}

type ChangeListener = (event: MediaQueryListEvent) => void

interface ActiveQuery {
  query: string
  listeners: Set<ChangeListener>
}

let viewportWidth = 0
const activeQueries = new Set<ActiveQuery>()

function toPixels(rawValue: string): number | null {
  const value = Number.parseFloat(rawValue)
  if (!Number.isFinite(value)) {
    return null
  }
  const unit = rawValue.trim().slice(-UNIT_LENGTH)
  return unit === 'px' ? value : value * EM_TO_PIXELS
}

function parseFeature(feature: string): MediaFeature | null {
  const [kind, rawValue] = feature.split(':')
  if (kind === undefined || rawValue === undefined) {
    return null
  }
  const pixels = toPixels(rawValue)
  if (pixels === null) {
    return null
  }
  return { atLeast: kind === 'min-width', pixels }
}

function matchesWidth(query: string, width: number): boolean {
  const feature = /\(([^)]+)\)/.exec(query)?.[1]
  if (feature === undefined) {
    return false
  }
  const parsed = parseFeature(feature)
  if (parsed === null) {
    return false
  }
  return parsed.atLeast ? width >= parsed.pixels : width <= parsed.pixels
}

function notify(active: ActiveQuery): void {
  const event = { matches: matchesWidth(active.query, viewportWidth) } as MediaQueryListEvent
  active.listeners.forEach((listener) => {
    listener(event)
  })
}

function trackListener(active: ActiveQuery, listener: ChangeListener): void {
  active.listeners.add(listener)
  activeQueries.add(active)
}

function untrackListener(active: ActiveQuery, listener: ChangeListener): void {
  active.listeners.delete(listener)
  if (active.listeners.size === 0) {
    activeQueries.delete(active)
  }
}

function createMediaQueryList(query: string): MediaQueryList {
  const active: ActiveQuery = { query, listeners: new Set() }
  const mediaQueryList = {
    get matches() {
      return matchesWidth(query, viewportWidth)
    },
    media: query,
    onchange: null,
    addEventListener: (type: string, listener: ChangeListener) => {
      if (type === 'change') {
        trackListener(active, listener)
      }
    },
    removeEventListener: (type: string, listener: ChangeListener) => {
      if (type === 'change') {
        untrackListener(active, listener)
      }
    },
    addListener: (listener: ChangeListener) => {
      trackListener(active, listener)
    },
    removeListener: (listener: ChangeListener) => {
      untrackListener(active, listener)
    },
    dispatchEvent: () => false,
  }
  return mediaQueryList as unknown as MediaQueryList
}

// Simulates a viewport for media queries expressed in px or em (16 px = 1 em).
// A query without a supported width feature never matches. Changing the width
// notifies mounted queries, so responsive hooks react like in a browser.
export function installViewport(width: number): void {
  viewportWidth = width
  activeQueries.forEach(notify)
  window.matchMedia = (query: string): MediaQueryList => createMediaQueryList(query)
}
