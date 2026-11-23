# 0004 — Biblioteki frontendu

- Status: accepted
- Date: 2026-11-21
- Superseded-by: none

Uzupełnienie (2026-11-21): [0006-api-contract.md](0006-api-contract.md) dodaje klienta API (zod, axios, openapi-typescript) i Day.js, a [0007-static-checks.md](0007-static-checks.md) dodaje ESLint, typescript-eslint i Prettier.

## Context

Frontend (panel i publiczna status page) to React + Vite + TypeScript ([0002-stack.md](0002-stack.md)). Pkt 1.5 backlogu: komponenty, formularze, wykresy, i18n, routing, dane, testy. Research: [frontend.md](../research/frontend.md). Zasada właściciela: nie pisać od zera tego, co jest sprawdzone; własny kod tylko dla rzeczy trywialnych.

## Decision

| Potrzeba | Wybór |
|---|---|
| Komponenty | Mantine 9 (`@mantine/core`, `@mantine/hooks`) z własnym motywem z tokenów [ui-ux.md](../specs/ui-ux.md); bez `@mantine/charts` |
| Formularze | `@mantine/form`; renderer formularzy z deskryptora typów checków po naszej stronie |
| Tekst incydentów i post-mortemów | `react-markdown` z wyłączonym surowym HTML |
| Wykresy | Chart.js (sparkline, wykres faz, historia) |
| Routing i stan w URL | TanStack Router |
| Dane i cache | TanStack Query 5 |
| i18n PL/EN | i18next + react-i18next |
| Aktualizacje live | natywny `EventSource` z fallbackiem na polling |
| Testy | Vitest + Testing Library |

### Rozmieszczenie kodu (2026-11-22)

- **`app/`:** wejście (`main.tsx`), providery (`App.tsx`), router, `QueryClient`, motyw, inicjalizacja i18n oraz szkielet panelu. To jedyne miejsce, które spina pozostałe katalogi.
- **`routes/`:** tylko trasy TanStack Router: walidacja parametrów, ochrona i render jednego widoku z funkcji. Trasy nie zawierają logiki funkcji.
- **`features/<nazwa>/`:** funkcja domenowa (dziś `auth`: logowanie, wylogowanie, `/api/v1/me`, sesja). Publiczne wejście to wyłącznie `index.ts` z jawnymi nazwanymi eksportami. W środku: `api/*.schemas.ts` (Zod), `api/*.api.ts` (transport i parsowanie), `api/*.queries.ts` (klucze i `queryOptions`), komponenty, testy i tłumaczenia funkcji.
- **`shared/`:** instancja axios i obsługa błędów HTTP bez wiedzy o domenie, neutralne komponenty UI, czyste narzędzia i wspólne tłumaczenia.
- **`test/`:** `renderWithProviders`-owy helper `renderApp`, testowy adapter axios i dane testowe.

Granice modułów pilnuje ESLint (`import-x/no-restricted-paths` i `import-x/no-cycle`) w `frontend/eslint.config.js`. `shared` nie importuje `app`, `routes` ani `features`, a `features` nie importują `app` ani `routes`. Macierz dozwolonych zależności między funkcjami żyje tylko w `frontend/eslint.config.js`; ten ADR jej nie kopiuje. Pliki testów są zwolnione z reguł granic i cykli.

## Consequences

- Wymagany React ≥ 19.2 (Mantine 9).
- Mantine ma jednego głównego opiekuna i major mniej więcej raz w roku; motyw trzymamy w jednym miejscu, żeby migracje były tanie.
- Testy w MVP nie obejmują automatycznych skanów dostępności (axe) ani E2E na telefonie, które wymienia backlog pkt 1.5; dostępność WCAG 2.2 AA sprawdzana ręcznie do czasu decyzji o Playwright/axe.
- Nowe zależności są tylko te z tabeli i z ADR 0006 i 0007. Każda kolejna wymaga zgody właściciela.

## Links

- [frontend.md](../research/frontend.md), [ui-ux.md](../specs/ui-ux.md), [backlog.md](../research/backlog.md)
