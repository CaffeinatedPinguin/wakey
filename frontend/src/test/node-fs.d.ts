// Vitest runs tests in Node, but the application tsconfig only includes Vite's
// client types. This minimal declaration covers the Node APIs the contract test
// uses; it keeps production code free of Node typing.
declare module 'node:fs' {
  export function readFileSync(path: string, encoding: 'utf8'): string
}

declare const process: {
  cwd(): string
}
