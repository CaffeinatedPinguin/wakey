# 0007 — Statyczne sprawdzanie kodu (Java i frontend)

- Status: accepted
- Date: 2026-11-21
- Superseded-by: none

## Context

Kod w dużej części pisze AI, a AI zapomina o zasadach i trzeba po nim poprawiać. Właściciel chce, żeby jak najwięcej reguł pilnował build, a nie review: każde złamanie to czerwony build. Formatowaniem AI nie powinno się zajmować wcale. Granice modułów pilnuje ArchUnit ([0005-server-architecture.md](0005-server-architecture.md)). Ten ADR dotyczy reguł samego kodu. Decyzje zapadły w grill-me 2026-11-21.

Jak robią inni: Gatus ma gofmt i go vet, OpenStatus ma TypeScript strict i Biome, Uptime Kuma i Checkmate mają ESLint. Wszyscy trzymają się jednego formattera i jednego lintera, bez nakładających się narzędzi.

## Decision

### Java (serwer)
- **Spotless + palantir-java-format.** Lokalny build formatuje sam (`spotless:apply`), CI tylko sprawdza (`spotless:check`). AI nie formatuje ręcznie.
- **Error Prone + NullAway.** Error Prone łapie ok. 500 wzorców błędów. NullAway wykrywa NPE w czasie kompilacji, korzystając z adnotacji JSpecify w Spring Framework 7 i Boot 4.
- **Checkstyle wyłącznie z regułami strukturalnymi.** Styl należy do formattera.

  | Reguła | Próg |
  |---|---|
  | `NeedBraces` | zawsze (`if`/`else`/`for`/`while`/`do`) |
  | `NestedIfDepth` | 1 (trzeci zagnieżdżony `if` to błąd) |
  | `NestedForDepth`, `NestedTryDepth` | 1 |
  | `ParameterNumber` | 4 (metody i konstruktory) |
  | `MethodLength` | 40 linii bez pustych |
  | `CyclomaticComplexity` | 10 |
  | `BooleanExpressionComplexity` | 3 |
  | `AvoidStarImport`, `EmptyCatchBlock`, `InnerAssignment` | włączone |

- Wyjątki od Checkstyle wolno dodawać tylko w jednym pliku `checkstyle-suppressions.xml`, żeby każdy był widoczny w diffie.
- **Świadomie bez:** javac `-Werror`, Maven Enforcer, SpotBugs, PMD i SonarQube. SpotBugs i PMD dublują Error Prone, a Sonar wymaga serwera.

### Frontend (React + TypeScript)
- TypeScript `strict` oraz `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes` i `noImplicitOverride`.
- **ESLint + typescript-eslint** z regułami opartymi na typach (np. `no-floating-promises`). Do tego zestaw z szablonu Vite: `@eslint/js`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh` i `globals`. Formatuje **Prettier**.
- Reguły strukturalne mają te same progi co w Javie: `curly` zawsze, `max-depth` 2, `max-params` 4, `max-lines-per-function` 40, `complexity` 10.
- Granice modułów pilnuje wbudowane `no-restricted-imports`. Publiczny jest tylko `features/<x>/index.ts`, tak samo jak pakiet główny modułu w Javie.
- ESLint działa z `--max-warnings 0`.

## Consequences

- Nowe zależności Javy (wtyczki builda i kompilatora): Spotless, palantir-java-format, Error Prone, NullAway, Checkstyle.
- Nowe zależności dev frontendu: eslint, @eslint/js, typescript-eslint, eslint-plugin-react-hooks, eslint-plugin-react-refresh, globals, prettier.
- Error Prone wymaga flag `--add-exports` w `.mvn/jvm.config` i wydłuża kompilację o ok. 10–20%. Wersję wspierającą JDK 25 trzeba potwierdzić w bramce zgodności na etapie 0.
- Nazwy właściwości Checkstyle i ESLint trzeba potwierdzić w dokumentacji wersji, którą dołączymy.
- Bez `-Werror` ostrzeżenia kompilatora nie blokują builda. Bez Enforcera zakazane zależności (np. webflux, JPA, OkHttp) wyłapuje tylko review i zasada „każda nowa zależność za zgodą właściciela”.

## Links

- [0005-server-architecture.md](0005-server-architecture.md), [0004-frontend.md](0004-frontend.md), [0006-api-contract.md](0006-api-contract.md)
