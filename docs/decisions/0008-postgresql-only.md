# 0008 — Tylko PostgreSQL

- Status: accepted
- Date: 2026-11-22
- Superseded-by: none

## Context

Do 2026-11-22 wakey wspierał dwie bazy: PostgreSQL i HSQLDB w trybie pliku. Utrzymanie dwóch
dialektów kosztowało dużo więcej niż dawało. Wspólny podzbiór SQL blokował funkcje, które są
potrzebne produktowi: kolejkę opartą o `LISTEN/NOTIFY` i `ON CONFLICT`, partycjonowanie wyników
pomiarów i agregacje liczone w SQL. Każdy test i każdy changeset trzeba było pisać i sprawdzać
dwa razy, a CI trzymało dwie ścieżki uruchomienia.

Self-host nadal ma być prosty. Zamiast plikowej bazy w procesie aplikacji dostaje `docker compose`
z usługą PostgreSQL i jedną komendą startową. To jedyny koszt tej decyzji: operator potrzebuje
kontenera bazy albo własnego PostgreSQL, a nie tylko jednego pliku.

Decyzja właściciela z 2026-11-22: wspieramy wyłącznie PostgreSQL. Warunek właściciela: każde
miejsce zależne od PostgreSQL ma jawny znacznik i wiersz w tabeli poniżej, a migracja na inną bazę
ma opisaną ścieżkę.

## Decision

- PostgreSQL jest jedyną obsługiwaną bazą: w chmurze i w self-hoście (przez `docker compose`).
- HSQLDB znika z zależności, kodu, testów, obrazu i obowiązujących zasad. Testy integracyjne
  korzystają z jednego wspólnego kontenera PostgreSQL na przebieg (`dev.wakey.testsupport`).
- Kontrakty lease i kolejki zostają interfejsami testowymi. Dziś mają po jednej implementacji na
  PostgreSQL; druga implementacja pojawi się dopiero razem z nową bazą.
- Każde miejsce, które działa tylko na PostgreSQL, ma w źródle komentarz zawierający
  `postgres-only: <krótko co i dlaczego>`. W Javie to `// postgres-only: ...`, w XML
  `<!-- postgres-only: ... -->`, w SQL `-- postgres-only: ...`, a w YAML `# postgres-only: ...`.
- Nowe miejsce `postgres-only` wymaga nowego wiersza w tabeli „Funkcje tylko PostgreSQL”
  w tym samym PR. Pilnuje tego test `PostgresOnlyRegistryTest`.
- Nie przepisujemy obecnego SQL na składnię PostgreSQL. Standardowy SQL (`MERGE`, `LIMIT`) zostaje.
  Od teraz wolno używać funkcji PostgreSQL, ale każda dostaje znacznik i wiersz w tabeli.

## Funkcje tylko PostgreSQL

| Funkcja | Plik | Co zmienić przy migracji |
|---|---|---|
| Właściwości limitów sesji `wakey.database.*` | `server/src/main/java/dev/wakey/config/DatabaseProperties.java` | Odwzorować limity w ustawieniach nowej bazy albo przenieść je do konfiguracji sterownika. |
| Szablon `options` i log limitów sesji | `server/src/main/java/dev/wakey/config/DatabaseSessionLimitsConfiguration.java` | Zastąpić `-c idle_in_transaction_session_timeout=... -c lock_timeout=...` odpowiednikiem nowej bazy. |
| Właściwość `options` pgjdbc z limitami sesji | `server/src/main/java/dev/wakey/config/PostgresSessionLimitsBeanPostProcessor.java` | Ustawić limity przez API nowego sterownika albo `SET` po otwarciu połączenia. |
| Typ `BYTEA` dla atrybutów sesji | `server/src/main/resources/db/changelog/changes/0003-spring-session.xml` | Zmienić typ na odpowiednik binarny nowej bazy (np. `BLOB`) i usunąć znacznik. |
| Konfiguracja limitów sesji | `server/src/main/resources/application.yml` | Przenieść `wakey.database.*` do konfiguracji nowej bazy i usunąć znacznik. |
| Natywne typy `UUID` i `TIMESTAMPTZ` w tabelach przestrzeni | `server/src/main/resources/db/changelog/changes/0004-spaces.xml` | Zmienić kolumny na typ binarny i znacznik czasu z offsetem nowej bazy i usunąć znacznik. |
| Natywne typy `UUID` i `TIMESTAMPTZ` w tabeli audytu | `server/src/main/resources/db/changelog/changes/0005-audit.xml` | Zmienić kolumny na typ binarny i znacznik czasu z offsetem nowej bazy i usunąć znacznik. |
| Natywne typy `UUID` i `TIMESTAMPTZ` w tabeli tokenów API | `server/src/main/resources/db/changelog/changes/0006-api-tokens.xml` | Zmienić kolumny na typ binarny i znacznik czasu z offsetem nowej bazy i usunąć znacznik. |
| Natywne typy `UUID` i `TIMESTAMPTZ` w tabeli ustawień przestrzeni | `server/src/main/resources/db/changelog/changes/0007-space-settings.xml` | Zmienić kolumny na typ binarny i znacznik czasu z offsetem nowej bazy i usunąć znacznik. |

## Migracja na inną bazę

1. Znajdź wszystkie miejsca ze znacznikiem: `grep -rn "postgres-only" server docs`.
2. Przejdź tabelę „Funkcje tylko PostgreSQL” i dla każdego wiersza przygotuj odpowiednik w nowej
   bazie. Znacznik usuwaj dopiero razem z wierszem tabeli.
3. Sprawdź changesety Liquibase i SQL w repozytoriach pod kątem typów oraz funkcji, których nowa
   baza nie ma. Każde takie miejsce dostaje znacznik i wiersz w tabeli.
4. Dodaj drugą implementację kontraktów `LeaderLease` i `MessageQueue` dla nowej bazy.
5. Uruchom oba kontrakty testowe na nowej bazie, a potem cały zestaw testów i build.
6. Zaktualizuj ten ADR: opisz nową bazę, zmień tabelę i dopisz, które funkcje PostgreSQL zniknęły.

## Consequences

- Self-host wymaga PostgreSQL, najprościej przez `docker compose`. Znika „jeden plik bazy” w procesie
  aplikacji i związany z nim limit pliku `.data`.
- CI i testy mają jedną ścieżkę bazy, więc są prostsze i szybsze.
- Wolno używać pełnego PostgreSQL: `ON CONFLICT`, `LISTEN/NOTIFY`, partycjonowania i agregacji w SQL.
  Każde takie użycie jest widoczne w rejestrze i w diffie.
- Migracja na inną bazę jest możliwa, ale to świadoma praca: rejestr mówi, co trzeba zastąpić,
  a kontrakty mówią, jak sprawdzić nową implementację.

## Links

- [0001-stack.md](0001-stack.md), [0002-stack.md](0002-stack.md)
- [0005-server-architecture.md](0005-server-architecture.md), [0007-static-checks.md](0007-static-checks.md)
- [roadmap.md](../specs/roadmap.md)
