// One placeholder substitution for every operation path from the contract:
// the caller names the values, so an id, a key and a slug travel the same way.
export function operationPath(template: string, params: Readonly<Record<string, string>>): string {
  return template.replace(/\{(\w+)\}/g, (placeholder, name: string) => {
    const value = params[name]
    return value === undefined ? placeholder : encodeURIComponent(value)
  })
}
