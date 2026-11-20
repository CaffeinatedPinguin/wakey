# Uptime monitor — dokumentacja projektu

Ten katalog zawiera dokumentację produktu, research i decyzje. Plan wdrożenia `uptime-monitor-plan.md` (v0.16) nie znajduje się w tym katalogu; jeśli go dodasz, wrzuć go do `docs/` i zaktualizuj odnośniki.

## Co jest czym

| Plik | Co to jest | Status |
|---|---|---|
| `docs/product-features.md` | Co robi produkt: funkcje komponentów, UI, role, etapy MVP/v1/później | v0.9 |
| `research/backlog.md` | Lista tematów do zbadania przed kolejnymi etapami; pkt 1 zamknięty | v1.1 |
| `research/ui-ux-brief.md` | Zlecenie dla researchu UI/UX (wejście) | v1 |
| `research/ui-ux-spec.md` | Wynik researchu UI/UX: specyfikacja zachowania, ekranów, komponentów i tokenów | v1.1 |
| `decisions/0001-stack.md` | ADR: Java/Spring Boot, bazy, agent Go, CI | przyjęty 2026-11-19 |

## Kolejność czytania

1. `docs/product-features.md` — co budujemy i dla kogo.
2. `research/ui-ux-spec.md` — jak ma wyglądać i zachowywać się UI.
3. `decisions/0001-stack.md` — na czym budujemy i dlaczego.
4. `research/backlog.md` — co jeszcze trzeba zbadać.

## Konwencje

- `docs/` — specyfikacje produktu.
- `research/` — backlog, briefy i wyniki researchu.
- `decisions/` — ADR numerowane `NNNN-temat.md`; bez folderu `done`.
- Plan wdrożenia: `uptime-monitor-plan.md` (poza tym katalogiem).

## Stare nazwy

| Stara nazwa | Nowa nazwa |
|---|---|
| `uptime-monitor-funkcje.md` | `docs/product-features.md` |
| `uptime-monitor-researche.md` | `research/backlog.md` |
| `decisions/done/1 uptime-monitor-brief-ui-ux.md` | `research/ui-ux-brief.md` |
| `decisions/done/1 DESIGN-brief-summary.md` | `research/ui-ux-spec.md` |
| `decisions/done/1 STACK-ADR.md` | `decisions/0001-stack.md` |
