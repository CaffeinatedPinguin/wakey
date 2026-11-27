// Zod gives every optional field the type `T | undefined`, while the generated
// contract under exactOptionalPropertyTypes keeps `T` on an optional property.
// This mirrors the contract shape so `satisfies z.ZodType<...>` still checks it.
type OptionalKeys<T> = {
  [K in keyof T]-?: object extends Pick<T, K> ? K : never
}[keyof T]

type RequiredKeys<T> = Exclude<keyof T, OptionalKeys<T>>

export type ApiShape<T> = T extends readonly (infer U)[]
  ? ApiShape<U>[]
  : T extends object
    ? { [K in RequiredKeys<T>]: ApiShape<T[K]> } & {
        [K in OptionalKeys<T>]?: ApiShape<T[K]> | undefined
      }
    : T
