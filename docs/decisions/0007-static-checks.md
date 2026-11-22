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
- **Świadomie bez:** Maven Enforcer, SpotBugs, PMD, SonarQube i javac `-Werror` — ostrzeżenia javac i domyślne ostrzeżenia Error Prone zostają ostrzeżeniami i nie przerywają builda. SpotBugs i PMD dublują Error Prone, a Sonar wymaga serwera. Zaostrzenie 2026-11-22 nie dodaje też żadnych nowych zależności, wtyczek ani pakietów npm i nie wyłącza reguł ani nie dodaje wpisów do `checkstyle-suppressions.xml`.

### Frontend (React + TypeScript)
- TypeScript `strict` oraz `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes` i `noImplicitOverride`.
- **ESLint + typescript-eslint** z regułami opartymi na typach (np. `no-floating-promises`). Do tego zestaw z szablonu Vite: `@eslint/js`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh` i `globals`. Formatuje **Prettier**.
- Reguły strukturalne mają te same progi co w Javie: `curly` zawsze, `max-depth` 2, `max-params` 4, `max-lines-per-function` 40, `complexity` 10.
- Granice modułów pilnuje wbudowane `no-restricted-imports`. Publiczny jest tylko `features/<x>/index.ts`, tak samo jak pakiet główny modułu w Javie.
- ESLint działa z `--max-warnings 0`.

### Zaostrzenie 2026-11-22

Właściciel zatwierdził wariant A: zaostrzamy reguły bez dodawania nowych zależności, wtyczek ani pakietów npm.

- **Error Prone 2.53.0.** Błędem są tylko błędy Error Prone oraz sprawdzenia jawnie ustawione na `ERROR` — `MissingOverride`, `UnusedVariable` i `FieldCanBeFinal`. `UnnamedVariable` to dodatkowa reguła na `ERROR`, która uzupełnia `UnusedVariable` (ta celowo pomija nazwy zaczynające się od `unused`); nie jest obejściem żadnego wyłączenia. Pozostałe domyślne ostrzeżenia Error Prone i ostrzeżenia samego javac zostają ostrzeżeniami i dzięki `showWarnings` są widoczne w logu, ale nie zatrzymują builda. Żadne sprawdzenie nie jest wyłączone.
- **NullAway 0.14.4.** Dołożone `-XepOpt:NullAway:JSpecifyMode=true` i `-XepOpt:NullAway:CheckOptionalEmptiness=true`; zostaje `AnnotatedPackages=dev.wakey` i `NullAway:ERROR`.
- **Checkstyle 14.4.0.** Nowe moduły i progi:

  | Reguła | Próg |
  |---|---|
  | `IllegalCatch`, `IllegalThrows` | domyślne listy |
  | `NPathComplexity` | 200 |
  | `ClassFanOutComplexity` | 20 |
  | `ReturnCount` | 2, dla `void` 1 |
  | `FileLength` | 400 linii |
  | `ParameterAssignment`, `ModifiedControlVariable` | włączone |
  | `MissingSwitchDefault`, `FallThrough` | włączone |
  | `EqualsHashCode`, `FinalClass`, `HideUtilityClassConstructor` | włączone; klasa z `@SpringBootApplication` jest wyjątkiem |
  | `VisibilityModifier`, `MutableException` | włączone |

- **ArchUnit 1.5.1.** Do istniejących reguł doszły stałe z `com.tngtech.archunit.library.GeneralCodingRules`: `NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS`, `NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS`, `NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING`, `NO_CLASSES_SHOULD_USE_FIELD_INJECTION` i `DEPRECATED_API_SHOULD_NOT_BE_USED`.
- **Frontend.** `recommendedTypeChecked` zastąpiono `strictTypeChecked` i `stylisticTypeChecked` (typescript-eslint 8.73.0). Dodano `@typescript-eslint/switch-exhaustiveness-check`, `@typescript-eslint/consistent-type-imports`, `@typescript-eslint/explicit-module-boundary-types` oraz core: `eqeqeq`, `no-console`, `no-param-reassign`, `no-nested-ternary`, `no-implicit-coercion`, `max-lines` 300, `max-statements` 15, `prefer-const`, `no-else-return`. Progi z 2026-11-21 zostają.
- Kod istniejący poprawiono bez wyłączania reguł: w testach Springa wstrzykiwanie `JdbcTemplate` przez pole zamieniono na konstruktor, a `App` ma jawny typ zwracany. `checkstyle-suppressions.xml` pozostał pusty.

## Consequences

- Nowe zależności Javy (wtyczki builda i kompilatora): Spotless, palantir-java-format, Error Prone, NullAway, Checkstyle.
- Nowe zależności dev frontendu: eslint, @eslint/js, typescript-eslint, eslint-plugin-react-hooks, eslint-plugin-react-refresh, globals, prettier.
- Error Prone wymaga flag `--add-exports` w `.mvn/jvm.config` i wydłuża kompilację o ok. 10–20%. Wersję wspierającą JDK 25 trzeba potwierdzić w bramce zgodności na etapie 0.
- Ostrzeżenia javac i domyślne ostrzeżenia Error Prone nie przerywają builda; build zatrzymują wyłącznie błędy i sprawdzenia jawnie ustawione na `ERROR`. Dzięki `showWarnings` wszystkie ostrzeżenia są widoczne w logu, więc naruszenia spoza listy błędów trzeba wyłapywać podczas review.
- Nazwy właściwości Checkstyle i ESLint trzeba potwierdzić w dokumentacji wersji, którą dołączymy.
- Bez Enforcera zakazane zależności (np. webflux, JPA, OkHttp) wyłapuje tylko review i zasada „każda nowa zależność za zgodą właściciela”.

## Links

- [0005-server-architecture.md](0005-server-architecture.md), [0004-frontend.md](0004-frontend.md), [0006-api-contract.md](0006-api-contract.md)
