# 0009 — Motyw jako standard aplikacji

- Status: accepted
- Date: 2026-11-23
- Superseded-by: none

## Context

Panel i publiczna status page muszą wyglądać jak jeden produkt: te same kolory, rozmiary, radius
i typografia w Mantine, w komponentach i w lekkim shellu status page. Bez wspólnego źródła każdy
ekran dokłada własne hexy i piksele, a jasny i ciemny motyw rozjeżdżają się. Właściciel przygotował
motyw (`--app-*`, marka, rozszerzenie `uptime`) i chce, żeby był standardem aplikacji, a nie
opcjonalnym dodatkiem ([ui-ux.md](../specs/ui-ux.md) 8).

## Decision

- **Motyw jest standardem.** Kod żyje w `frontend/src/shared/theme/`, a `MantineProvider` w `app/`
  używa `createAppTheme`. Domyślny schemat to `auto` (jasny i ciemny według systemu).
- **Warstwy: core → marka → rozszerzenie domenowe.** `composeTokens` składa je w tej kolejności.
  Rozszerzenie nie może nadpisać tokenu core ani marki ani zdefiniować tokenu, który ma już inne
  rozszerzenie; wtedy rzuca błąd. Jeden system nie zmieni po cichu drugiego.
- **Marka nigdy nie dotyka stanów.** `createBrandTokens` sam koryguje kontrast (tekst akcentu,
  tekst na przycisku, obrys fokusu) i zwraca listę korekt oraz ostrzeżenie, gdy odcień marki jest
  bliski kolorowi stanu.
- **Tokeny bez zależności.** `tokens/*` i `color.ts` nie importują Mantine; lekki shell status page
  może użyć samego arkusza tokenów.
- **Zero tekstów w motywie.** Etykiety (np. `close`) przychodzą z aplikacji przez `labels`.
  `createAppTheme` wymaga `labels`; motyw nie ma wartości domyślnej, która po cichu wygrywa z i18n.
- **Zasady dla komponentów.** Kolory, rozmiary, radius i typografia wyłącznie z tokenów `--app-*`
  albo z theme Mantine. Stan to zawsze `<Badge color="up|degraded|down|unknown|maintenance">`
  z ikoną i tekstem — nigdy sam kolor. `danger` jest zarezerwowany dla akcji destrukcyjnych,
  nie dla stanu. Teksty UI tylko przez i18n (PL/EN).
- **Mobile jest wymaganiem.** Każdy ekran działa na telefonie (< 768 px, układ wg
  [ui-ux.md](../specs/ui-ux.md) 3.3), cele dotyku mają 44 px z `base.css`, a akcje kryzysowe
  (potwierdzenie, przejęcie) 48 px.

### Świadome odstępstwa

Macierz zgodności i uzasadnienia są w [theme.md](../specs/theme.md) (sekcje 3–4). Najważniejsze:

| Obszar | Odstępstwo | Uzasadnienie |
|---|---|---|
| Przycisk domyślny | 36 px, nie 40–44 px (`UI_UX_RULES` 16) | Narzędzie operacyjne z tabelami po tysiące wierszy; na dotyku zawsze 44 px |
| Body panelu | 14 px, nie 16 px (`UI_UX_RULES` 12) | Zwarty panel; pola na dotyku mają 16 px, żeby iOS nie powiększał widoku |
| Radius | 5 / 6 / 10 px dla kontrolek, kart i okien (`UI_UX_RULES` 13) | Decyzja właściciela 2026-11-23: ciaśniejszy, bardziej techniczny wygląd |
| Obrys pól | `border-default` (3,41:1 w jasnym, 3,87:1 w ciemnym) | WCAG 2.2 AA 1.4.11: granica pola musi być rozpoznawalna |
| `selection-hover` w ciemnym | 16% akcentu, nie 18% | Przy 18% tekst akcentu miał 4,50:1, dokładnie na granicy |
| Paleta stanów | Zieleń, pomarańcz, czerwień, błękit i łupek ze spec 8.1 | Jedna paleta w całym produkcie |
| DEGRADED solid w jasnym | `#CC6F05` | Poprawka kontrastu: poprzedni `#D97706` miał 2,95:1 na tle strony w jasnym |

## Consequences

- Lint (`eslint.config.js`) odrzuca w `app/`, `features/`, `routes/` i `shared/ui/` literały heksowe
  oraz jednostki `px`; kod ma używać tokenów albo wartości theme. Komunikat wskazuje `--app-*`.
- Kontrast sprawdza test `every text and control pair meets WCAG AA in light and dark` w `pnpm test`
  (wszystkie pary, oba motywy, marka domyślna i rozszerzenie `uptime`).
- Zmiana tokenu wymaga uruchomienia testu kontrastu i przeglądu
  [theme-preview.html](../specs/theme-preview.html) w obu motywach.
- Nowa zależność frontendu nadal wymaga zgody właściciela ([0004-frontend.md](0004-frontend.md)).

## Links

- [theme.md](../specs/theme.md), [ui-ux.md](../specs/ui-ux.md), [ui-ux-rules.md](../specs/ui-ux-rules.md), [0004-frontend.md](0004-frontend.md)
