# 0010 — Komponenty od etapu 4

- Status: accepted
- Date: 2026-11-26
- Superseded-by: none

## Context

Spec opisuje komponent przy status page (sekcja 5), a status page przychodzi w etapie 5. Incydenty
z etapu 4 już potrzebują komponentów: incydent zapisuje dotknięte komponenty, a incydent
automatyczny powstaje, gdy komponent przejdzie w `DOWN` ([product-features.md](../specs/product-features.md),
sekcja 4).

Gdyby etap 4 wiązał incydenty z checkami, etap 5 musiałby przepiąć zapisaną historię incydentów
na komponenty. Takiego przepięcia nie da się zrobić wiernie: nie wiadomo, który komponent
istniałby w chwili incydentu (reguła 3 w [roadmap.md](../specs/roadmap.md)).

## Decision

- Etap 4 wprowadza moduł `components` w minimalnej postaci: nazwa, przestrzeń i powiązane checki
  z tej samej przestrzeni.
- Stan komponentu wylicza czysta polityka w `components.domain`: najgorszy stan powiązanych
  checków. `UNKNOWN` i oznaczenie „do sprawdzenia konfiguracji” nigdy nie dają `DOWN`.
- Lider wylicza stan jako idempotentny konsument zmian stanu checków i publikuje zmianę stanu
  komponentu przez port kolejki ([0005](0005-server-architecture.md)) tylko przy faktycznej zmianie.
- Kierunek zależności: `incidents → components → checks`; w etapie 5 `statuspages → components,
  incidents`. Moduł `checks` nie zależy od komponentów.
- Etap 5 dokłada grupy komponentów, ręczne nadpisanie stanu i powiązanie ze status page.

## Consequences

- Incydenty od pierwszego dnia zapisują komponenty, więc historia na status page w etapie 5 nie
  wymaga migracji danych.
- Etap 4 ma jeden ekran więcej (lista i formularz komponentów) i jedną falę więcej (4b).
- Incydent automatyczny działa tylko dla checków powiązanych z komponentem. Tak samo opisuje to spec.

## Links

- [roadmap.md](../specs/roadmap.md) · [product-features.md](../specs/product-features.md) · [0005-server-architecture.md](0005-server-architecture.md)
