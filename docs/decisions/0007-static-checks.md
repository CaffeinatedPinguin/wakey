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

### Lombok (2026-11-22)

Właściciel zatwierdził Lombok 1.18.48 w ograniczonym zakresie. Dozwolone są tylko `@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@NoArgsConstructor(access = AccessLevel.PRIVATE)`, `@Builder` i `@Slf4j`. Zablokowane są m.in. `@Data`, `@Value`, `@EqualsAndHashCode`, `@ToString`, `@AllArgsConstructor`, `@With`, `@WithBy`, `@SneakyThrows`, `val`/`var`, `@Cleanup`, `@Synchronized`, `@Locked`, `@Accessors`, `@FieldDefaults`, `@UtilityClass`, `@FieldNameConstants`, `@StandardException`, `@SuperBuilder`, `@Jacksonized`, `@ExtensionMethod`, `@Helper`, `@NonNull`, `onX` oraz wszystkie loggery poza `@Slf4j`. Powód: `@Data`, `@Value` i `@EqualsAndHashCode` ukrywają `equals`/`hashCode`, a pozostałe skróty chowają kontrolę przepływu albo generują kod, którego nie widać w review. Blokadę egzekwuje `lombok.config` w korzeniu repo przez `flagUsage = ERROR` przy kompilacji; Lombok jest w `annotationProcessorPaths` obok Error Prone i NullAway, a `lombok.addLombokGeneratedAnnotation = true` sprawia, że NullAway i Error Prone pomijają kod generowany.

Testy integracyjne korzystają z jednego współdzielonego kontenera i jednego kontekstu Springa na bazę. Wzorzec to singleton container: statyczne pole z kontenerem w abstrakcyjnej klasie bazowej w `dev.wakey.testsupport`, start w bloku statycznym i `@ServiceConnection`. Nie używamy `@Testcontainers` ani `@Container`, bo ich cykl życia zatrzymuje współdzielony kontener po pierwszej klasie testowej.

### Wyjątki bez śladu (2026-11-22)

Żaden wyjątek nie może zniknąć po cichu, ale dwa świadome wyjątki są dozwolone: pusty `catch` z komentarzem (np. `// ignore`) oraz log z `e.getMessage()`. Poza tym blok `catch` w kodzie produkcyjnym serwera musi albo rzucić wyjątek dalej (z przyczyną), albo przekazać złapany wyjątek do wywołania, np. loggera (`LOG.warn("…", e)`), tak żeby w logu był stack trace. Bez nowych zależności, bez `-Werror` i bez wyłączania reguł.

- **Error Prone `UnusedException` na `ERROR`** odrzuca `throw new X("komunikat")` w `catch`, które gubi złapany wyjątek zamiast przekazać go jako przyczynę (`throw new X("komunikat", e)`).
- **Checkstyle `EmptyCatchBlock`** (jeden moduł, domyślne ustawienia) odrzuca pusty `catch` bez komentarza; pusty `catch` z komentarzem jest świadomie dozwolony.
- **Checkstyle `MatchXpath`** pomija puste bloki `catch` i bloki zawierające tylko komentarze, a odrzuca niepusty `catch` bez `throw` i bez wywołania metody, w którego argumentach występuje zmienna złapanego wyjątku. Łapie więc `return null` oraz `LOG.warn("…")`, a przepuszcza `LOG.warn("…", e)`, `LOG.warn("…", e.getMessage())`, `throw e` i `throw new X("…", e)`.

Czego te reguły nie łapią: logowania na złym poziomie (np. `debug` zamiast `error`), braku lub bezsensowności komunikatu logu oraz tego, że samo `e.getMessage()` bywa puste. Globalna obsługa nieoczekiwanych wyjątków w HTTP (`@RestControllerAdvice` z logiem) przyjdzie z pierwszymi endpointami na etapie 1.

### Struktura testów (2026-11-22)

Testy czytamy jak zdania o zachowaniu, nie jak skróty implementacji.

- Nazwa testu to zdanie o zachowaniu, bez przedrostków `should`/`test` (np. `expiredLeaseIsTakenOverWithAHigherToken`).
- Jeden test sprawdza jedno zachowanie; asercje mówią o wymaganiu, a nie powtarzają implementacji.
- Test dłuższy niż jedna instrukcja dzieli się komentarzami `// given` (przygotowanie), `// when` (akcja) i `// then` (asercje); `// given` pomijamy, gdy nie ma przygotowania.
- Kontrakt, który musi spełniać każda implementacja na każdej bazie, to interfejs testowy JUnit (`LeaderLeaseContract`, `MessageQueueContract`). Wspólne bazy i dostęp do beanów mieszkają w `dev.wakey.testsupport`, a klasy konkretne tylko wybierają bazę danych.
- Ekonomia (AHA, DRY, KISS): testujemy wymagania, nie implementację; wspólne pomocniki tylko dla wiedzy, która musi zmieniać się razem; jednorazowych pomocników nie wyciągamy; klasy łączące bazę z kontraktem nie dostają rozbudowanego Javadoc.

Regułę pilnuje `config/checkstyle/checkstyle-test.xml`, uruchamiany drugą egzekucją `maven-checkstyle-plugin` w fazie `verify` tylko dla katalogu testów (`server/src/test/java`).

### Proste argumenty (2026-11-22)

W całym kodzie, produkcyjnym i testowym, argument wywołania zawiera najwyżej jedno wywołanie metody. Dłuższe wyrażenie trafia do zmiennej z nazwą. Łańcuch wywołań na obiekcie (`builder.a(x).b(y)`, `jdbc.sql(...).param(...)`) i ciała lambd są dozwolone, bo reguła liczy tylko wywołania wewnątrz jednego argumentu.

Przed:

```java
assertThat(results.stream().filter(Optional::isPresent).count()).isEqualTo(1L);
```

Po:

```java
long winners = results.stream().filter(Optional::isPresent).count();
assertThat(winners).isEqualTo(1L);
```

Reguły pilnują `config/checkstyle/checkstyle.xml` (kod produkcyjny) i `config/checkstyle/checkstyle-test.xml` (testy).

### Zamknięte zbiory wartości we frontendzie (2026-11-25)

Zamknięty zbiór napisów (stany, role, schematy kolorów, kody błędów) to jeden obiekt stałych z typem o tej samej nazwie, nigdy unia literałów rozpisana w miejscu użycia ani `enum`:

```ts
export const SpaceRole = { EDITOR: 'EDITOR', VIEWER: 'VIEWER' } as const
export type SpaceRole = (typeof SpaceRole)[keyof typeof SpaceRole]

function canEdit(role: SpaceRole): boolean {
  return role === SpaceRole.EDITOR
}
const roleSchema = z.enum(SpaceRole)
```

- **Dlaczego nie unia `'a' | 'b'`:** ten sam zbiór powtarza się w typach, testach i schematach Zoda, a zmiana wartości wymaga szukania napisów po całym kodzie.
- **Dlaczego nie `enum`:** `tsconfig` ma `erasableSyntaxOnly`, więc kod musi dać się uruchomić po samym wycięciu typów (esbuild w Vite, type stripping w Node), a `enum` generuje kod. Do tego string `enum` jest nominalny: napis z JSON-a API nie pasuje do niego bez rzutowania.
- Obiekt stałych działa jak `enum` (`SpaceRole.EDITOR`, podpowiedzi), a napis z API pasuje do typu bez rzutowania. `Object.values(SpaceRole)` daje listę wartości, np. do testów i pętli.
- Zbiór z kontraktu API generuje `generate-api.mjs`, a nie pisze się go ręcznie. Typ z biblioteki (np. `MantineColorScheme`) używamy wprost.
- Pojedynczy literał (`scheme: 'light'` jako wartość) i klucze obiektów są dozwolone; reguła dotyczy typu, który wylicza dozwolone napisy.

Regułę pilnuje `no-restricted-syntax` w `frontend/eslint.config.js`: unia typów z literałem napisowym oraz `z.enum([...])` z tablicą literałów są błędem w kodzie produkcyjnym i w testach. Pliki generowane (`src/shared/api/generated/`) są wyłączone.

## Consequences

- Nowe zależności Javy (wtyczki builda i kompilatora): Spotless, palantir-java-format, Error Prone, NullAway, Checkstyle.
- Nowe zależności dev frontendu: eslint, @eslint/js, typescript-eslint, eslint-plugin-react-hooks, eslint-plugin-react-refresh, globals, prettier.
- Error Prone wymaga flag `--add-exports` w `.mvn/jvm.config` i wydłuża kompilację o ok. 10–20%. Wersję wspierającą JDK 25 trzeba potwierdzić w bramce zgodności na etapie 0.
- Ostrzeżenia javac i domyślne ostrzeżenia Error Prone nie przerywają builda; build zatrzymują wyłącznie błędy i sprawdzenia jawnie ustawione na `ERROR`. Dzięki `showWarnings` wszystkie ostrzeżenia są widoczne w logu, więc naruszenia spoza listy błędów trzeba wyłapywać podczas review.
- Nazwy właściwości Checkstyle i ESLint trzeba potwierdzić w dokumentacji wersji, którą dołączymy.
- Bez Enforcera zakazane zależności (np. webflux, JPA, OkHttp) wyłapuje tylko review i zasada „każda nowa zależność za zgodą właściciela”.

## Links

- [0005-server-architecture.md](0005-server-architecture.md), [0004-frontend.md](0004-frontend.md), [0006-api-contract.md](0006-api-contract.md)
