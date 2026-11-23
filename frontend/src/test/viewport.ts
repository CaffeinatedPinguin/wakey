const EM_TO_PIXELS = 16
const UNIT_LENGTH = 2

interface MediaFeature {
  atLeast: boolean
  pixels: number
}

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

// Simulates a viewport for media queries expressed in px or em (16 px = 1 em).
// A query without a supported width feature never matches.
export function installViewport(width: number): void {
  window.matchMedia = (query: string): MediaQueryList => {
    const mediaQueryList = {
      matches: matchesWidth(query, width),
      media: query,
      onchange: null,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      dispatchEvent: () => false,
    }
    return mediaQueryList as unknown as MediaQueryList
  }
}
