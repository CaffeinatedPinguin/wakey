# Research: technologie frontendu wakey (React + Vite + TypeScript, pkt 1.5)

**Stan:** 2026-11-20. Wersje z npm `latest`; rozmiary min+gzip z Bundlephobii; liczba zależności przechodnich z deps.dev albo z rejestru npm. Stack React + Vite + TypeScript przyjmuję z ADR 0001 i nie podważam.

## Zakres

Badam osiem potrzeb z pkt 1.5: komponenty UI i dostępność, formularze ze schematu, wykresy przy 5 000 checków, i18n PL/EN, routing i stan w URL oraz cache danych, aktualizacje live (SSE, polling, snapshot), testy (axe, wydajność, telefon) i bezpieczne formatowanie treści. Dla każdej potrzeby porównuję wariant „platforma/własny kod” z co najmniej jedną biblioteką. Poza zakresem: implementacja SSE w Spring Boot, projekt graficzny i makiety, hosting/CDN, biblioteki logowania i płatności, pinowanie wersji produkcyjnych oraz szczegółowy schemat deskryptora checków — to decyzje etapu 1.

## Opcje

### 1. Komponenty UI

- **Platforma/własne:** komponenty na tokenach CSS z `ui-ux.md` §8; trudne wzorce na elementach natywnych: `<dialog>` (modal, focus, `inert`, `::backdrop`, Baseline od 2022) i Popover API (menu, tooltip, toasty, Baseline od 2025), `<details>` do akordeonów. Źródła: [dialog](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/dialog), [popover](https://developer.mozilla.org/en-US/docs/Web/API/Popover_API), [WCAG 2.2](https://www.w3.org/TR/WCAG22/).
- **React Aria Components 1.22.1** (Apache-2.0): dostępne ComboBox, Menu, Tooltip, Dialog; 8 zależności bezpośrednich, 12 przechodnich; 279 kB gzip dla całego pakietu (nie dla pojedynczych importów). [npm](https://registry.npmjs.org/react-aria-components/latest) [deps.dev](https://api.deps.dev/v3alpha/systems/npm/packages/react-aria-components/versions/1.22.1:dependencies) [Bundlephobia](https://bundlephobia.com/package/react-aria-components@1.22.1).
- **Radix Primitives 1.8.1** (MIT): 55 zależności bezpośrednich w pakiecie zbiorczym (73 przechodnich), 74,3 kB gzip dla całości; pojedyncze paczki `@radix-ui/react-*` zmniejszają koszt, ale nie dają comboboxa z filtrowaniem. [npm](https://registry.npmjs.org/radix-ui/latest) [Bundlephobia](https://bundlephobia.com/package/radix-ui@1.8.1).
- **Ark UI / Zag 1.48.0** (MIT): maszyny stanu bez stylów; wrapper React 4 zależności bezpośrednie, 4,2 kB gzip; mniejsza społeczność niż React Aria. [npm](https://registry.npmjs.org/@zag-js/react/latest) [Bundlephobia](https://bundlephobia.com/package/@zag-js/react@1.48.0).
- **Pełny kit** (MUI, Ant Design, Chakra): wariant odniesienia — wymaga nadpisania motywu, tokenów i gęstości tabel, a wnosi setki kB i własny system stylów.

### 2. Formularze ze schematu

- **Platforma/własne:** `SchemaForm` renderuje pola z deskryptora JSON; walidacja inline na Constraint Validation API (`required`, `pattern`, `min/max`, `setCustomValidity`), serwer pozostaje autorytetem. [MDN](https://developer.mozilla.org/en-US/docs/Web/HTML/Guides/Constraint_validation).
- **React Hook Form 7.92.0** (MIT, 0 zależności, 16,4 kB gzip) — stan i błędy pól; nie zna „źródła wartości” ani „Przywróć”. [npm](https://registry.npmjs.org/react-hook-form/latest).
- **TanStack Form 1.33.5** (MIT, 2 bezpośrednie/6 przechodnich, 17,9 kB gzip). [npm](https://registry.npmjs.org/@tanstack/react-form/latest).
- **Zod 4.6.7 / Valibot 1.5.0** (MIT, 0 zależności, 88,9 / 14,7 kB gzip) — walidacja schematów TypeScript, nie zastępuje deskryptora z backendu. [npm zod](https://registry.npmjs.org/zod/latest) [npm valibot](https://registry.npmjs.org/valibot/latest).

### 3. Wykresy

- **Platforma/własne:** sparkline jako inline SVG (24 punkty, ok. 60 linii), pasek historii jako 90 elementów SVG; brak zależności. Preagregacja po stronie API, nie w przeglądarce.
- **uPlot 1.6.32** (MIT, 0 zależności, 21,9 kB gzip) — jeden wykres faz w szczegółach checka. [npm](https://registry.npmjs.org/uplot/latest) [Bundlephobia](https://bundlephobia.com/package/uplot@1.6.32).
- **Chart.js 4.6.1** (MIT, 1/1, 69,6 kB gzip) — canvas, wygodne osie, ale większy. [npm](https://registry.npmjs.org/chart.js/latest).
- **Recharts 3.11.0** (MIT, 11/≈22, 176,7 kB gzip) — deklaratywny React, dużo zależności (d3, redux). [npm](https://registry.npmjs.org/recharts/latest) [Bundlephobia](https://bundlephobia.com/package/recharts@3.11.0).
- **ECharts 6.1.0** (Apache-2.0, 2/2, 368,0 kB gzip) — najbogatszy, ale ciężki i kanciasty w React. [npm](https://registry.npmjs.org/echarts/latest) [Bundlephobia](https://bundlephobia.com/package/echarts@6.1.0).

### 4. i18n PL/EN

- **Platforma/własne:** słownik PL/EN + `Intl.PluralRules` (polskie `one/few/many/other`), `Intl.RelativeTimeFormat` („6 minut temu”), `Intl.DateTimeFormat` ze `timeZone` przestrzeni, `Intl.NumberFormat`; 0 zależności. [PluralRules](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/PluralRules) [RelativeTimeFormat](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/RelativeTimeFormat).
- **i18next 27.2.2 + react-i18next 18.0.3** (MIT, 0+1 bezpośrednich, 15,6+10,5 kB gzip) — dojrzałe, ale częste majory i własny format zasobów. [i18next](https://registry.npmjs.org/i18next/latest) [react-i18next](https://registry.npmjs.org/react-i18next/latest).
- **intl-messageformat 12.2.0** (BSD-3-Clause, 2/3, 10,0 kB gzip) — pełny ICU MessageFormat dla tłumaczy. [npm](https://registry.npmjs.org/intl-messageformat/latest).

### 5. Routing, stan w URL i dane

- **Platforma/własne:** router na History API + `URLSearchParams` (ok. 250 linii); cache `fetch` z `AbortController`, `staleTime` i wiekiem danych. [URLSearchParams](https://developer.mozilla.org/en-US/docs/Web/API/URLSearchParams).
- **React Router 8.5.2** (MIT, 2/2, 64,8 kB gzip) — znany, obsługuje zagnieżdżone layouty i `searchParams`. [npm](https://registry.npmjs.org/react-router/latest) [Bundlephobia](https://bundlephobia.com/package/react-router@8.5.2).
- **TanStack Router 1.170.46** (MIT, 4/9, 31,0 kB gzip) — typowany routing i walidacja parametrów. [npm](https://registry.npmjs.org/@tanstack/react-router/latest) [Bundlephobia](https://bundlephobia.com/package/@tanstack/react-router@1.170.46).
- **TanStack Query 5.108.2** (MIT, 1/1, 13,4 kB gzip) — cache, retry, deduplikacja, odnawianie; **SWR 2.5.1** (MIT, 2/2, 5,7 kB gzip) — mniejszy, mniej funkcji. [npm query](https://registry.npmjs.org/@tanstack/react-query/latest) [npm swr](https://registry.npmjs.org/swr/latest) [Bundlephobia query](https://bundlephobia.com/package/@tanstack/react-query@5.108.2).

### 6. Aktualizacje live i snapshot

- **Platforma/własne:** `EventSource` (Baseline od 2020) + własny backoff, `visibilitychange`, fallback na polling i snapshot w Cache API/localStorage. Jeden strumień na kartę, nie osobny per widget. [EventSource](https://developer.mozilla.org/en-US/docs/Web/API/EventSource).
- **@microsoft/fetch-event-source 2.0.1** (MIT, 0 zależności, 1,4 kB gzip) — ostatnie wydanie 2021-04; daje POST i nagłówki, ale jest nieutrzymywany. [npm](https://registry.npmjs.org/@microsoft/fetch-event-source/latest).
- **WebSocket** (platforma): dwukierunkowy, ale wymaga więcej pracy serwera i własnego protokołu; dla jednokierunkowych zmian stanu koszt nieuzasadniony.

### 7. Testy

- **Platforma/własne:** `node:test` (wbudowany w Node) do logiki, ręczne skrypty w Chromium/DevTools do E2E, `PerformanceObserver` do pomiarów; zero zależności, ale brak emulacji telefonu, raportów i skanów WCAG.
- **Vitest 5.1.1** (MIT, dev) — jednostkowe i logika (formatowanie, parser, agregacja). [npm](https://registry.npmjs.org/vitest/latest).
- **Playwright 1.65.1** (Apache-2.0, dev) — E2E, emulacja telefonu, pomiary czasu; pobiera przeglądarki poza npm. [npm](https://registry.npmjs.org/@playwright/test/latest).
- **@axe-core/playwright 4.14.0 + axe-core 4.14.1** (MPL-2.0, dev) — automatyczne skany WCAG na kluczowych ekranach. [npm](https://registry.npmjs.org/@axe-core/playwright/latest) [axe-core](https://registry.npmjs.org/axe-core/latest).
- **Jest** i **Cypress** (MIT) — działają, ale dublują Vitest/Playwright i zwiększają utrzymanie; **Storybook** poza MVP.

### 8. Bezpieczne formatowanie tekstu

- **Platforma/własne:** tokenizer podzbioru Markdown (nagłówki, listy, pogrubienie, kursywa, link, kod) → React elements; zero `dangerouslySetInnerHTML` i zero HTML od użytkownika.
- **marked 18.2.1** (MIT, 0 zależności, 15,4 kB gzip) + **DOMPurify 3.4.18** (MPL-2.0 lub Apache-2.0, 0 zależności, 11,5 kB gzip). [marked](https://registry.npmjs.org/marked/latest) [DOMPurify](https://registry.npmjs.org/dompurify/latest).
- **react-markdown 10.1.0** (MIT, 11/≈53, 35,5 kB gzip) — renderuje do Reacta, ale z `rehype-raw` nadal wymaga sanitizera. [npm](https://registry.npmjs.org/react-markdown/latest).

## Porównanie

Kryteria KISS: licencja, zależności (bezpośrednie/przechodnie), rozmiar, aktywność i ryzyko na 10 lat. „Własne” = brak pakietu runtime; koszt to kod i utrzymanie.

Historia zmian (do ryzyka): React Aria 3.x bez majora od lat; React Router 6→7→8; i18next i Vitest z serią szybkich majorów; uPlot 1.6.x od 2021; TanStack Query 5.x i TanStack Router 1.x stabilne w obecnych majorach.

| Opcja | Wersja | Licencja | Zależności (bezp./przech.) | min+gzip | Ostatnie wydanie | Opiekunowie | Ryzyko 10 lat |
|---|---:|---|---:|---:|---|---:|---|
| Własne + natywne | — | — | 0/0 | 0 | — | 1 | niskie; koszt a11y po naszej stronie |
| React Aria Components | 1.22.1 | Apache-2.0 | 8/12 | 279 (entrypoint) | 2026-10 | 2 (Adobe) | średnie; duży chunk, stabilne API |
| Radix Primitives | 1.8.1 | MIT | 55/73 (całość) | 74,3 (całość) | 2026-10 | 2 | średnie; brak comboboxa, dużo paczek |
| Ark UI / Zag | 1.48.0 | MIT | 4/≈6 | 4,2 (wrapper) | 2026-11 | 1 | wyższe; jeden główny opiekun |
| Własny SchemaForm | — | — | 0/0 | 0 | — | 1 | niskie; walidacja po naszej stronie |
| React Hook Form | 7.92.0 | MIT | 0/0 | 16,4 | 2026-11 | 1 | niskie; mały, dojrzały |
| TanStack Form | 1.33.5 | MIT | 2/6 | 17,9 | 2026-08 | 3 | średnie; młodszy ekosystem |
| Zod / Valibot | 4.6.7 / 1.5.0 | MIT | 0/0 | 88,9 / 14,7 | 2026-10 / 2026-09 | 1 / 1 | średnie; nie pasuje do JSON-a z API |
| Własne i18n + Intl | — | — | 0/0 | 0 | — | 1 | niskie; testy form mnogich |
| i18next + react-i18next | 27.2.2 / 18.0.3 | MIT | 1/1 | 26,1 | 2026-11 | 2 | średnie; częste majory |
| intl-messageformat | 12.2.0 | BSD-3-Clause | 2/3 | 10,0 | 2026-11 | 3 | niskie; tylko formatowanie |
| Własny router + cache | — | — | 0/0 | 0 | — | 1 | niskie; ok. 400 linii do utrzymania |
| React Router | 8.5.2 | MIT | 2/2 | 64,8 | 2026-11 | 2 | średnie; majory 6→7→8 |
| TanStack Router | 1.170.46 | MIT | 4/9 | 31,0 | 2026-10 | 3 | średnie; szybkie zmiany API |
| TanStack Query | 5.108.2 | MIT | 1/1 | 13,4 | 2026-11 | 3 | niskie/średnie; stabilne v5 |
| SWR | 2.5.1 | MIT | 2/2 | 5,7 | 2026-08 | 3 | niskie; mniej funkcji |
| Własny EventSource | — | — | 0/0 | 0 | — | 1 | niskie; ok. 120 linii |
| fetch-event-source | 2.0.1 | MIT | 0/0 | 1,4 | 2021-04 | 6 | wysokie; projekt porzucony |
| Własny sparkline SVG | — | — | 0/0 | 0 | — | 1 | niskie; ok. 60 linii |
| uPlot | 1.6.32 | MIT | 0/0 | 21,9 | 2025-03 | 1 | średnie; jeden opiekun, wolne wydania |
| Chart.js | 4.6.1 | MIT | 1/1 | 69,6 | 2026-10 | 5 | niskie; duża społeczność |
| Recharts | 3.11.0 | MIT | 11/≈22 | 176,7 | 2026-11 | 3 | średnie; ciężki, d3 + redux |
| ECharts | 6.1.0 | Apache-2.0 | 2/2 | 368,0 | 2026-05 | 8 | niskie/średnie; duży bundle |
| Własny parser Markdown | — | — | 0/0 | 0 | — | 1 | niskie; podzbiór, testy XSS |
| marked + DOMPurify | 18.2.1 / 3.4.18 | MIT / MPL-2.0 lub Apache-2.0 | 0/0 | 26,9 | 2026-11 | 4 / 1 | średnie; sanitizer to powierzchnia ataku |
| react-markdown | 10.1.0 | MIT | 11/≈53 | 35,5 | 2025-03 | 3 | wyższe; duże drzewo, osobna sanitizacja |

### Testy

| Narzędzie | Wersja | Licencja | Zależności (bezp./przech.) | Rola | Ryzyko |
|---|---:|---|---:|---|---|
| Vitest | 5.1.1 | MIT | 12/≈79 (graf 4.0.0: 92 węzły) | unit, logika, parser | średnie; częste majory, duże drzewo dev |
| Playwright | 1.65.1 | Apache-2.0 | 1/3 | E2E, telefon, wydajność | niskie; stabilne API, ciężkie przeglądarki w CI |
| @axe-core/playwright + axe-core | 4.14.0 / 4.14.1 | MPL-2.0 | 1/1 | skany WCAG | niskie; wykrywa część problemów, nie zastępuje przeglądu |

### Preagregacja wykresów

| Widok | Bucket | Punktów na serię |
|---|---|---:|
| Sparkline w liście (24 h) | 1 h | 24 |
| Szczegóły 24 h | 5 min | 288 |
| Szczegóły 7 dni | 1 h | 168 |
| Szczegóły 30 dni | 6 h | 120 |
| Szczegóły 60 dni | 12 h | 120 |
| Szczegóły 365 dni | 1 dzień | 365 |

Dla wykresu faz API zwraca na bucket `p50` i `p95` osobno dla DNS, TCP, TLS i TTFB oraz liczbę prób i błędów. Wykres rysuje maksymalnie 365 punktów na serię, lista zawsze 24. Brak danych to przerwa w linii i jawny stan `UNKNOWN`, nie zero.

## Własna implementacja

Szacunki dla jednej osoby, ±50%; „trudne” to miejsca, gdzie łatwo o regresję dostępności lub bezpieczeństwa.

| Element | Linie | Dni | Trudne | Testy |
|---|---:|---:|---|---|
| Tokeny CSS + 14 komponentów z §6 | 1200–1800 | 10–15 | focus, `aria-live`, tabela na telefonie | axe + Playwright + przegląd klawiaturą |
| `ServerDataTable` (paginacja, zaznaczanie, karty) | 400–600 | 4–6 | zaznacz stronę vs wynik, kolejność pod kursorem | Playwright, testy stanu URL |
| `Sparkline` SVG | ≈60 | 0,5 | skalowanie, brak danych | snapshoty + dane skrajne |
| `PhaseLatencyChart` na uPlot | ≈150 | 2–3 | osie, tooltip, alternatywa tabelaryczna | Playwright + testy formatowania |
| `SchemaForm` + `SourceValue` | 600–900 | 6–9 | progressive disclosure, źródło i „Przywróć”, walidacja inline | testy deskryptora, axe |
| i18n `t()` + formatery | ≈150 | 1–2 | formy mnogie PL, strefa czasowa | testy jednostkowe dla 0/1/2/5/22 |
| Router i stan w URL | ≈250 | 2–3 | back/forward, scroll, focus po nawigacji | Playwright + testy URL |
| Cache danych (jeśli bez Query) | ≈150 | 2 | retry, anulowanie, wiek danych | testy z mockiem `fetch` |
| Hook SSE + fallback polling | ≈120 | 1–2 | backoff, `Last-Event-ID`, przełączanie na polling | testy timera, Playwright |
| `SafeRichText` (parser → React) | 250–350 | 3–4 | schematy linków, zagnieżdżenie, XSS | testy jednostkowe + fuzz |

Nie pisać samemu: mechaniki comboboxa z `aria-activedescendant`, listboxa wielokrotnego wyboru i pozycjonowania tooltipa — to miejsca, gdzie biblioteka headless wygrywa z KISS. Warto napisać samemu: dialog (natywny `<dialog>`), akordeon (`<details>`), menu/Toast na Popover API, badge, banner, pasek historii, sparkline, parser tekstu i formatery `Intl`.

## Rekomendacja

1. **UI:** własne komponenty na tokenach + natywne `<dialog>`/popover; **React Aria Components** tylko dla ComboBox, Menu i Tooltip, ładowany lazy w panelu (status page go nie potrzebuje). Uzasadnienie: combobox i menu to najdroższe wzorce a11y, a natywny dialog pokrywa `ScopeConfirmDialog` bez zależności.
2. **Formularze:** własny `SchemaForm` + Constraint Validation API; bez React Hook Form i bez Zod. Deskryptor przychodzi z backendu, a walidacja biznesowa i tak musi być na serwerze.
3. **Wykresy:** własny sparkline SVG (24 punkty) + **uPlot** dla wykresu faz; preagregacja po stronie API według tabeli wyżej. Bez Recharts i ECharts.
4. **i18n:** własne `t()` + `Intl.PluralRules`/`RelativeTimeFormat`/`DateTimeFormat`. Bez i18next; format ICU można dodać później, jeśli tłumacze tego zażądają.
5. **Routing i dane:** **React Router 8** (URL, layouty, `searchParams`) + **TanStack Query** (cache, retry, wiek danych, polling). Własny router tylko jako wariant zero-zależnościowy.
6. **Live:** natywny `EventSource` + własny wrapper (backoff, jeden strumień na kartę, fallback polling, snapshot z `generatedAt`). Bez `fetch-event-source` (porzucony).
7. **Testy:** **Vitest** (logika) + **Playwright** (E2E, telefon, wydajność) + **@axe-core/playwright** (WCAG). Bez Jest, Cypress, Storybooka i Testing Library w MVP.
8. **SafeRichText:** własny parser podzbioru Markdown renderujący do React elements; bez `marked`/DOMPurify i bez `react-markdown`. To eliminuje całą klasę błędów sanitizacji.

React, ReactDOM, Vite i TypeScript są już przyjęte w ADR 0001, więc nie wchodzą do listy „wprowadzonych”. Poniżej wszystkie pozostałe zależności rekomendacji.

**dependencies**

| Pakiet | Wersja | Licencja | Zależności przechodnie | Po co |
|---|---:|---|---:|---|
| react-aria-components | 1.22.1 | Apache-2.0 | 12 | ComboBox, Menu, Tooltip; a11y trudnych wzorców |
| react-router | 8.5.2 | MIT | 2 | routing, layouty, stan filtrów w URL |
| @tanstack/react-query | 5.108.2 | MIT | 1 | cache, retry, deduplikacja, wiek danych |
| uplot | 1.6.32 | MIT | 0 | wykres faz odpowiedzi per lokalizacja |

**devDependencies**

| Pakiet | Wersja | Licencja | Zależności przechodnie | Po co |
|---|---:|---|---:|---|
| vitest | 5.1.1 | MIT | ≈79 (graf 4.0.0: 92 węzły) | testy jednostkowe logiki i parserów |
| @playwright/test | 1.65.1 | Apache-2.0 | 3 | E2E, emulacja telefonu, pomiary wydajności |
| @axe-core/playwright | 4.14.0 | MPL-2.0 | 1 (axe-core) | automatyczne skany WCAG w E2E |

## Ryzyka

| Ryzyko | Mitygacja |
|---|---|
| React Aria Components wnosi 279 kB gzip w pełnym entrypoincie | import tylko ComboBox/Menu/Tooltip, `React.lazy` w panelu, budżet bundle w CI |
| React Router zmienia majory (6→7→8), a wersja 8 wymaga React ≥19.2.7 | pin wersji, sprawdzenie wersji React na etapie 0, aktualizacja raz na kwartał, testy E2E jako siatka bezpieczeństwa |
| uPlot ma jednego opiekuna i wolne wydania | warstwa `PhaseLatencyChart` za własnym interfejsem, fallback do własnego SVG |
| Własny parser Markdown rozjedzie się z oczekiwaniami | jawny podzbiór w UI, testy XSS i fuzz, brak `dangerouslySetInnerHTML` |
| Własne i18n pominie formę mnogą | testy `Intl.PluralRules` dla PL (1/2/5/22) i EN, przegląd tłumaczeń |
| SSE bez HTTP/2 limituje połączenia (6 na domenę) | jeden strumień na kartę, HTTP/2 na serwerze, fallback polling |
| Snapshot status page ujawni stare dane jako aktualne | `generatedAt` w UTC, wiek liczony po stronie klienta, jawny baner trybu awaryjnego |
| Playwright wydłuża CI i pobiera przeglądarki | cache przeglądarek, tylko kluczowe przepływy, axe na 5 ekranach |
| Testy axe dają fałszywe poczucie zgodności | axe + przegląd klawiaturą i czytnikiem ekranu przed wydaniem |

## Pytania do grill-me

1. Czy akceptujesz React Aria Components jako jedyną bibliotekę UI (12 zależności przechodnich) tylko dla ComboBox/Menu/Tooltip, czy wolisz własny combobox bez zależności i z większym ryzykiem a11y?
2. Router: React Router 8 (64,8 kB gzip, 2 zależności) czy własny router na History API (ok. 250 linii, zero zależności)?
3. Czy TanStack Query ma być warstwą cache i retry (13,4 kB, 1 zależność), czy wystarczy własny `fetch` z `AbortController`?
4. Czy uPlot może być jedyną biblioteką wykresów (21,9 kB, jeden opiekun), czy wolisz własny SVG także dla wykresu faz?
5. Czy `SafeRichText` ma być własnym parserem podzbioru Markdown (bez HTML), czy dopuszczasz `marked` + DOMPurify dla większej zgodności z Markdownem?
6. Czy w CI uruchamiamy Playwright z axe (kilka minut, pobrane przeglądarki), czy tylko axe w testach jednostkowych i ręczny przegląd?
7. Czy wykresy muszą być w pełni obsługiwane klawiaturą, czy wystarczy równoważna tabela danych i podsumowanie tekstowe?
8. Czy status page może korzystać z Cache API/service workera do przeżycia awarii API, czy wystarczy snapshot w pamięci i `localStorage`?
9. Czy tłumacze potrzebują składni ICU (plural/select) już w MVP, czy wystarczy klucz + kategoria mnogości wybrana przez `Intl.PluralRules`?
10. Czy limity planu mają ukrywać część komponentów panelu (np. wykres faz), żeby zmniejszyć bundle dla małych instalacji?

## Źródła

- https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/dialog
- https://developer.mozilla.org/en-US/docs/Web/API/Popover_API
- https://www.w3.org/TR/WCAG22/
- https://developer.mozilla.org/en-US/docs/Web/HTML/Guides/Constraint_validation
- https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/PluralRules
- https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/RelativeTimeFormat
- https://developer.mozilla.org/en-US/docs/Web/API/URLSearchParams
- https://developer.mozilla.org/en-US/docs/Web/API/EventSource
- https://registry.npmjs.org/react-aria-components/latest
- https://registry.npmjs.org/radix-ui/latest
- https://registry.npmjs.org/@zag-js/react/latest
- https://api.deps.dev/v3alpha/systems/npm/packages/react-aria-components/versions/1.22.1:dependencies
- https://bundlephobia.com/package/react-aria-components@1.22.1
- https://bundlephobia.com/package/radix-ui@1.8.1
- https://bundlephobia.com/package/@zag-js/react@1.48.0
- https://registry.npmjs.org/react-hook-form/latest
- https://registry.npmjs.org/@tanstack/react-form/latest
- https://registry.npmjs.org/zod/latest
- https://registry.npmjs.org/valibot/latest
- https://bundlephobia.com/package/react-hook-form@7.92.0
- https://bundlephobia.com/package/@tanstack/react-form@1.33.5
- https://registry.npmjs.org/uplot/latest
- https://registry.npmjs.org/chart.js/latest
- https://registry.npmjs.org/recharts/latest
- https://registry.npmjs.org/echarts/latest
- https://bundlephobia.com/package/uplot@1.6.32
- https://bundlephobia.com/package/recharts@3.11.0
- https://bundlephobia.com/package/echarts@6.1.0
- https://registry.npmjs.org/i18next/latest
- https://registry.npmjs.org/react-i18next/latest
- https://registry.npmjs.org/intl-messageformat/latest
- https://bundlephobia.com/package/intl-messageformat@12.2.0
- https://registry.npmjs.org/react-router/latest
- https://registry.npmjs.org/@tanstack/react-router/latest
- https://registry.npmjs.org/@tanstack/react-query/latest
- https://registry.npmjs.org/swr/latest
- https://bundlephobia.com/package/react-router@8.5.2
- https://bundlephobia.com/package/@tanstack/react-query@5.108.2
- https://bundlephobia.com/package/@tanstack/react-router@1.170.46
- https://bundlephobia.com/package/swr@2.5.1
- https://registry.npmjs.org/@microsoft/fetch-event-source/latest
- https://bundlephobia.com/package/@microsoft/fetch-event-source@2.0.1
- https://registry.npmjs.org/vitest/latest
- https://registry.npmjs.org/@playwright/test/latest
- https://registry.npmjs.org/@axe-core/playwright/latest
- https://registry.npmjs.org/axe-core/latest
- https://api.deps.dev/v3alpha/systems/npm/packages/vitest/versions/4.0.0:dependencies
- https://api.deps.dev/v3alpha/systems/npm/packages/@playwright/test/versions/1.60.0:dependencies
- https://api.deps.dev/v3alpha/systems/npm/packages/@axe-core/playwright/versions/4.11.0:dependencies
- https://registry.npmjs.org/marked/latest
- https://registry.npmjs.org/dompurify/latest
- https://registry.npmjs.org/react-markdown/latest
- https://bundlephobia.com/package/marked@18.2.1
- https://bundlephobia.com/package/dompurify@3.4.18
- https://bundlephobia.com/package/react-markdown@10.1.0
