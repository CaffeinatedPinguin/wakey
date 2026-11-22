# 0001 — Stack backendu, baz i agenta

- Status: superseded
- Date: 2026-11-19
- Superseded-by: 0002

Zastąpiony 2026-11-21 przez [0002-stack.md](0002-stack.md).

Fragmenty o HSQLDB i wspólnym podzbiorze SQL: Zastąpione przez [0008](0008-postgresql-only.md) (2026-11-22). Obowiązuje tylko PostgreSQL, a zasady wspólnego podzbioru SQL nie są już wiążące.

- Zakres: etap 0 i dalsze
- Dokumenty: `docs/research/backlog.md` (v1.1), `docs/specs/product-features.md` (v0.9)

## Context

Produkt: open source uptime monitor (self-host i chmura w UE), jeden frontend React + Vite, backend Java, tysiące checków, agenci w sieciach prywatnych, PostgreSQL w chmurze i HSQLDB jako opcja self-host. Utrzymuje jedna osoba. Wymagania: prostota, mały VPS, brak zależności zewnętrznych w self-host, dwie bazy w CI, agent bez uprawnień administratora.

## Decision

### Backend

- Java 25 LTS.
- Spring Boot w najnowszym stabilnym 4.x. Dokładna wersja wybrana na etapie 0 po sprawdzeniu zgodności springdoc, Liquibase, Testcontainers i 
- Maven (Maven Wrapper). Multi-module: parent + `server` (+ wspólny moduł serwerowy, jeśli potrzebny). Agent to osobny moduł Go poza Mavenem; UI osobno.
- Jedna aplikacja Spring Boot z rolami włączanymi konfiguracją (`app.roles`), warunkowe beany. Self-host domyślnie wszystkie role w jednym procesie; chmura może je rozdzielić.

### Baza i migracje

> Zastąpione przez [0008](0008-postgresql-only.md) (2026-11-22): obowiązuje tylko PostgreSQL, a wspólny podzbiór SQL nie jest już wymagany. Poniższe zasady zostają jako historia decyzji.

- Spring Data  dla agregatów; `JdbcClient`/`JdbcTemplate` dla batchy, raportów i zapytań o dużej objętości. Bez JPA/Hibernate.
- Encje konfiguracyjne i domenowe: UUID generowane w aplikacji. Spring Data JDBC wymaga `Persistable`/`@Version` do rozpoznania nowej encji, inaczej `save()` wykona UPDATE. Do potwierdzenia testem.
- Wyniki pomiarów: klucz złożony (`check_id`, `ts`) albo BIGINT identity; decyzja w projekcie schematu, testowana na obu bazach.
- Liquibase XML, forward-only. Zmiany `dbms`-specyficzne niedozwolone. Przed migracją zrzut bazy zgodnie z funkcjami.
  - Układ changelogów (2026-11-22): `db.changelog-master.xml` tylko importuje pliki zmian przez `<include file="..." relativeToChangelogFile="true"/>` w jawnej kolejności (bez `includeAll`), a każdy changeset jest w osobnym pliku `changes/NNNN-<opis>.xml` z rosnącą numeracją. Nic nie zostało jeszcze wydane, więc zmiana ścieżki pliku — części tożsamości changesetu w Liquibase — nie wymaga migracji istniejących baz. Od pierwszego wydania pliku changesetu nie wolno przenosić ani zmieniać.
- Bazy: PostgreSQL w chmurze i większych instalacjach; HSQLDB w trybie pliku i jednego procesu jako opcja self-host. Wybór konfiguracją przy uruchomieniu. HSQLDB server mode poza MVP.
- Wspólny podzbiór SQL:
  - JSON jako CLOB/TEXT, walidacja w aplikacji.
  - UUID z aplikacji; typ natywny, jeśli testy na obu bazach potwierdzą identyczne zachowanie, inaczej `CHAR(36)`.
  - Czasy `TIMESTAMP WITH TIME ZONE` w UTC.
  - Identyfikatory bez cytowania, nazwy bez zależności od case.
  - Zakaz: `ON CONFLICT`, `MERGE`, `ILIKE`, partycjonowania, advisory locków, tablic, enumów, indeksów częściowych.
  - Dozwolone: `LIMIT/OFFSET`, `LOWER(col) LIKE`, `SELECT ... FOR UPDATE`.
  - Upsert: atomowy `UPDATE`, przy 0 wierszy `INSERT` + retry po unique.
  - Cały SQL w warstwie repozytoriów; testy na obu bazach.

### Executor i lider

- Executor checków: wątki wirtualne; harmonogram na 1–2 wątkach platformowych; semafor globalny i per lokalizacja; ograniczona kolejka wyników; batchowany zapis; pula połączeń DB niezależna od liczby wątków. Bez WebFlux.
- Pinning w JDBC/Hikari i DNS oraz zachowanie przy 5 000 checków sprawdzają testy pisane razem z executorem.
- Leader lock: własna tabela `leader_lock(name, holder_id, fencing_token, expires_at)`, atomowy `UPDATE ... WHERE`, czas z bazy, TTL 30 s i odnowienie co 10 s (konfigurowalne). Brak odnowienia = natychmiastowe zaprzestanie pracy jako lider. Token fencing przekazywany do zadań; zadania idempotentne.

### CI

- GitHub Actions: unit testy raz; testy integracyjne dwa razy (HSQLDB in-memory + smoke na pliku, PostgreSQL w Testcontainers); Liquibase na obu. Testy tylko repozytoriów, migracji i krytycznych przepływów.

### Agent

- Go, statyczna binarka `CGO_ENABLED=0`, amd64/arm64.
- MVP: Linux (binarka) i Docker; Windows/macOS później.
- Bez automatycznych aktualizacji. Podpisane wydania, powiadomienie o nowej wersji, aktualizacja ręczna (podmiana binarki + restart).
- CA certyfikaty i tzdata: systemowe albo w obrazie; scratch/distroless.
- ICMP: `x/net/icmp` udp4/udp6, zależność od `ping_group_range`; Docker `--sysctl`/`CAP_NET_RAW`; fallback TCP. Do potwierdzenia w pkt 2.
- Kontrakt JSON z serwerem; wspólne wektory testowe dla asercji HTTP, kodów błędów i kategorii; testy parity z executorem Java.
- Serwer nie przenosi executora checków do Go; duplikacja semantyki ograniczona kontraktem i testami.

### Frontend

- React + Vite + TypeScript (ustalone).
- Biblioteka komponentów, formularze ze schematu, wykresy, i18n, cache status page: research 1.5. Do tego czasu brak wiążących decyzji.

## Uzasadnienie

- Java 25 LTS + Spring Boot 4.x: długie wsparcie, dojrzały ekosystem, wątki wirtualne, jeden build z rolami.
- Spring Data JDBC: jawny SQL bez magii ORM, łatwiejszy wspólny podzbiór; `JdbcClient` dla wolumenu.
- Liquibase XML: jeden changelog dla obu baz; CI wychwytuje różnice.
- HSQLDB w pliku: self-host bez zewnętrznych usług; PostgreSQL dla skali i chmury.
- Wątki wirtualne: tysiące checków I/O tanio; brak potrzeby reaktywnego stosu.
- Lease + fencing: poprawność przy utracie połączenia i split-brain; działa na obu bazach.
- Go agent: brak JRE, mały ślad, najlepsza kontrola nad DNS/ICMP/fazami HTTP; koszt drugiego toolchainu akceptowalny dla małego, izolowanego agenta.
- CI matrix: dryf dialektów wychodzi w PR, nie na produkcji.

## Consequences

- Dwa toolchainy (Java/Maven i Go), dwa release pipeline'y; kontrakt JSON i testy parity.
- HSQLDB: jeden proces, brak partycjonowania/HA; limit skali do zmierzenia i opisania; PostgreSQL rekomendowany powyżej.
- Migracje forward-only; backup przed migracją.
- Dokładne wersje i zgodność ekosystemu sprawdza fala, która dodaje daną zależność.
- Frontend czeka na 1.5; do tego czasu nie wybieramy bibliotek UI/wykresów.

## Ryzyka i mitygacje

| Ryzyko | Mitygacja |
|---|---|
| Luki ekosystemu Boot 4 | Testy zgodności przy dodaniu zależności; ostatnia zgodna wersja 4.x; pin |
| Dryf dialektów SQL | Wspólny podzbiór, SQL tylko w repozytoriach, testy na obu bazach |
| Skala HSQLDB | Benchmark, dokumentowany limit, ścieżka PostgreSQL |
| Pinning wątków w JDBC/DNS | Testy przy executorze, testy obciążeniowe |
| Parity Go/Java | Wspólne wektory testowe i kontrakt JSON |
| Dwa toolchainy | CI z cache, mały zakres agenta, jedna osoba utrzymuje oba |
| Ręczne aktualizacje | Podpisane wydania, powiadomienie o wersji, procedura aktualizacji |

## Co sprawdzają testy pisane razem z funkcją

1. Wersje: Java 25, Boot 4.x, springdoc, Liquibase, Testcontainers, Spring Data JDBC, Maven Wrapper; pin.
2. Liquibase na HSQLDB (plik) i PostgreSQL (Testcontainers): UUID, `TIMESTAMP WITH TIME ZONE`, CLOB, identity/generated keys, `SELECT FOR UPDATE`, `LIMIT/OFFSET`, `LOWER ... LIKE`.
3. Spring Data JDBC: nowa encja z UUID (`Persistable`/`@Version`), batch przez `JdbcClient`, testy na obu bazach.
4. Wątki wirtualne: HttpClient, JDBC/Hikari, DNS; testy pinningu i obciążeniowe przy executorze (5 000 checków).
5. Leader lease: acquire/renew/expire na obu bazach, utrata połączenia, fencing.
6. Go agent: statyczna binarka, CA/tzdata, httptrace, własny resolver, łańcuch TLS, ICMP bez roota (Linux/Docker), `--network=host`, reconnect/buffer, wektory parity.
7. Szkielet CI matrix i test HSQLDB file backup/restore.

## Otwarte

- Frontend: pkt 1.5.
- Dokładne wersje po bramce etapu 0.
- Domyślne TTL lease, rozmiary batchy, pula połączeń, limit skali HSQLDB.
- Klucz tabeli wyników pomiarów (złożony vs BIGINT identity).
- Windows/macOS agent: później.
- Roadmapa etapów: plan v0.16 nie istnieje w repo; do odtworzenia (`docs/research/backlog.md`, pkt 0).

## Links

- `docs/research/backlog.md` (v1.1), pkt 1, 1.5, 2.
- `docs/specs/product-features.md` (v0.9), sekcje 0, 2, 12.
- `docs/specs/ui-ux.md` (UI/UX).
