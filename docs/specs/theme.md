# Theme — tokeny, marka i rozszerzenia

Motyw jest standardem aplikacji ([0009-theme.md](../decisions/0009-theme.md)): jedno źródło kolorów,
rozmiarów, radiusa i typografii dla panelu i publicznej status page. Wartości tokenów są decyzją
właściciela i nie zmieniają się przy przenoszeniu kodu.

## 1. Użycie

```tsx
import { createAppTheme, uptimeExtension } from '../shared/theme'

const { theme, cssVariablesResolver, brandAdjustments } = useMemo(
  () =>
    createAppTheme({
      brand: { accent: '#7367F0' }, // opcjonalnie
      extensions: [uptimeExtension],
      labels: { close: t('common.close') },
    }),
  [locale],
)

<MantineProvider theme={theme} cssVariablesResolver={cssVariablesResolver} defaultColorScheme="auto">
```

- `labels` jest wymagane: motyw nie zawiera tekstów UI. Przy zmianie języka motyw tworzy się
  ponownie (`useMemo` zależne od języka).
- Status: `<Badge color="down" leftSection={<IconOctagonX size={14} />}>Nie działa</Badge>`.
- Segment, kropka, linia: `var(--app-state-down-solid)` z tekstem `var(--app-state-down-on-solid)`.
- Ogólne komunikaty: `color="success" | "warning" | "info" | "neutral"`. Kolor `danger` jest
  zarezerwowany dla akcji destrukcyjnych, nie dla stanu.
- Nowe rozszerzenie: obiekt `ThemeExtension` (tokeny `shared`/`light`/`dark` + `semanticColors`)
  i test kontrastu w `pnpm test`.
- Fonty: aplikacja importuje `@fontsource/geist` (400/500/600) i `@fontsource/jetbrains-mono`
  (400/500).

## 2. Architektura plików

```
frontend/src/shared/theme/
  color.ts                  matematyka kolorów i kontrast WCAG (bez zależności)
  tokens/core.shared.ts     rozmiary, radius, odstępy, typografia, layout, motion
  tokens/core.light.ts      powierzchnie, tekst, statusy — jasny
  tokens/core.dark.ts       powierzchnie, tekst, statusy — ciemny
  tokens/core.roles.ts      role semantyczne nadpalety (input, nav, selection)
  tokens/core.ts            coreTokens + tła, na których musi działać marka
  tokens/brand.ts           kolor marki → akcent z gwarantowanym kontrastem + ostrzeżenia
  tokens/compose.ts         core + marka + rozszerzenia; tokensToCss dla systemów bez Mantine
  tokens/extension.ts       kontrakt ThemeExtension
  tokens/types.ts           TokenMap, SchemeTokens, rem
  extensions/uptime.ts      stany UP/DEGRADED/DOWN/UNKNOWN/MAINTENANCE, HistoryBar, admin
  mantine/createAppTheme.ts theme Mantine z tokenów (createAppTheme, readToken)
  mantine/theme.ts          bazowe opcje createTheme
  mantine/colors.ts         paleta Mantine z tokenów
  mantine/components.ts     Styles API komponentów
  mantine/styles.ts         wspólne style kontrolek i nakładek
  mantine/variant.ts        resolvery wariantów i zmienne kontrolek
  mantine/bridge.ts         mapowanie tokenów na zmienne Mantine
  mantine/cssVariables.ts   cssVariablesResolver
  mantine/types.ts          AppThemeLabels, SemanticMap
  base.css                  reset, fokus, dotyk 44 px, reduced motion, forced colors
  index.ts                  jawne nazwane eksporty
```

Zasady, dzięki którym motyw da się utrzymać:

1. **Warstwy:** core → marka → rozszerzenie domenowe. Rozszerzenie nie może nadpisać tokenu core
   ani marki ani zdefiniować tokenu, który ma już inne rozszerzenie. `composeTokens` rzuca wtedy
   błąd.
2. **Marka nigdy nie dotyka statusów.** `createBrandTokens` sam koryguje kontrast i zwraca listę
   korekt oraz ostrzeżenie, gdy odcień marki jest bliski kolorowi stanu.
3. **Tokeny bez zależności.** `tokens/*` i `color.ts` nie importują Mantine, więc lekki shell
   status page może użyć samego `tokensToCss`.
4. **Bez tekstów w motywie.** `labels.close` przychodzi z aplikacji (PL/EN). Motyw nie ma
   domyślnej etykiety.
5. **Stabilny kontrakt nazw.** Znaczenie istniejących tokenów się nie zmienia: m.in.
   `border-default` ma ≥ 3:1.

## 3. Macierz zgodności

Legenda: ✅ spełnione, ⚠️ świadome odstępstwo (uzasadnione), ⏳ do zrobienia w komponentach.

| Wymaganie | Źródło | Stan | Jak |
|---|---|---|---|
| Jasny i ciemny, PL/EN | ui-ux 0.3 | ✅ | `labels` z aplikacji, bez tekstów w motywie |
| WCAG 2.2 AA: tekst ≥ 4,5, grafika i kontrolki ≥ 3 | ui-ux 0.3, 8.5 | ✅ | test kontrastu: wszystkie pary w obu motywach, także dla obcych marek |
| Stan nigdy tylko kolorem | ui-ux 6.1, 8.1 | ✅ / ⏳ | motyw daje znacznik i pusty segment z obrysem; ikony stanów dobiera komponent |
| 5 stanów, pigułka + solid, reguła tekstu na wypełnieniu | ui-ux 2.1, 8.1 | ✅ | `extensions/uptime.ts` aliasuje statusy core |
| UNKNOWN łupkowy, nie czerwony | ui-ux 1.2, 2.3 | ✅ | `neutral` = łupek |
| Marka nie nadpisuje stanów, kontrola kontrastu marki | ui-ux 4.9 | ✅ | `createBrandTokens`: korekty + ostrzeżenie o bliskim odcieniu |
| Administracja: odmienny akcent | ui-ux 3.2 | ✅ | `admin-accent` (jasny poprawiony z 4,37 do 6,0:1) |
| Cel dotyku 44–48 px na telefonie, akcje kryzysowe 48 px | ui-ux 3.3, 5.4, 8.5 | ✅ | `base.css` przy `pointer: coarse` / `< 64em`; `control-critical` 48 |
| Typografia 24–28 / 18–20 / 15–16 / 14 / 12–13, mono dla danych | ui-ux 8.3 | ✅ | bez tekstu poniżej 12 px |
| `tabular-nums` dla czasów i liczników | ui-ux 8.3 | ✅ | Badge, Table, NumberInput, `td`, `time`, `.app-num` |
| Elastyczne szerokości dla PL | ui-ux 8.3 | ✅ | żadna kontrolka nie ma sztywnej szerokości |
| Skala odstępów 4–32 | ui-ux 8.4 | ✅ | + 48 / 64 / 96 z `ui-ux-rules.md` |
| `prefers-reduced-motion`, zmiana na żywo bez reorderu | ui-ux 4 | ✅ | `[data-live-changed]` = błysk 600 ms, wyłączany |
| Fokus widoczny ≥ 3:1 | ui-ux 8.5 | ✅ | 2 px akcentu z odstępem 2 px |
| Radius 6–8 kontrolki, 8–12 karty, 12–16 okna, pill tylko celowo | ui-ux-rules 13 | ⚠️ | **5 / 6 / 10** (decyzja właściciela 2026-11-23: ciaśniejszy, techniczny wygląd), pill tylko dla stanu |
| Przycisk domyślny 40–44 px na desktopie | ui-ux-rules 16 | ⚠️ | **36 px.** Narzędzie operacyjne z tabelami po tysiące wierszy; 40 px to `size="md"`, na dotyku zawsze 44 |
| Body 16 px | ui-ux-rules 12 | ⚠️ | **14 px w panelu.** 16 px dla treści do czytania i status page, a pola na dotyku 16 px |
| Jasny motyw nie jako odwrócony ciemny | ui-ux-rules 8 | ✅ | własne neutrale, cienie dwuwarstwowe, białe powierzchnie na szarej stronie |
| Tryb wysokiego kontrastu (Windows) | best practice | ✅ | `forced-colors`: obrys Badge i przycisków |
| Linki nawigacji nie przemalowane | ui-ux | ✅ | `a { color: inherit }`; linki w treści przez `Anchor` / `.app-link` |

## 4. Konflikty rozstrzygnięte

1. **Obrys pól formularza.** WCAG 2.2 AA 1.4.11 wymaga rozpoznawalnej granicy pola, więc pole ma
   `border-default` (3,87:1) zamiast cichego obrysu poniżej 3:1.
2. **`selection-hover` w ciemnym:** 16% akcentu zamiast 18%. Przy 18% tekst akcentu miał 4,50:1,
   dokładnie na granicy, i test kontrastu go odrzucał.
3. **Kolory statusów:** jedna paleta ze spec 8.1 (zieleń, pomarańcz, czerwień, błękit, łupek),
   żeby w produkcie nie było dwóch zieleni.
4. **DEGRADED solid w jasnym:** `#D97706` → `#CC6F05`; poprzednia wartość miała 2,95:1 na tle strony.
5. **Radius 5 / 6 / 10 px:** świadome odstępstwo od `ui-ux-rules.md` 13 — decyzja właściciela
   2026-11-23.

## 5. Otwarte kwestie

1. MAINTENANCE (niebieski) obok fioletu marki: rozróżnia je ikona. Jeśli w makietach będą się mylić,
   MAINTENANCE można przesunąć w stronę cyjanu.
2. Rozszerzenie drugiej domeny (poza uptime) dostaje własny plik w `extensions/` i wpis w teście
   kontrastu.
3. Density toggle (wiersz 36 px) po makietach listy checków.
