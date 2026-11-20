# Research: bazy danych, dostęp do bazy i migracje (wakey)

## Zakres

Badam sześć potrzeb z pkt 1 backlogu: (1) baza produkcyjna i self-host, (2) przechowywanie wyników pomiarów i retencja, (3) dostęp do bazy z JVM (z krótkim odniesieniem do Go), (4) migracje, (5) kopie zapasowe i odtwarzanie, (6) blokada lidera. Zakres obejmuje wolumen dla 5 000 checków i kilku lokalizacji oraz porównanie z Uptime Kuma, OpenStatus, Gatus, Healthchecks, Checkmate, Statping-ng i Upptime. Poza zakresem: benchmark na konkretnym VPS, wybór dostawcy chmury i cennik usług zarządzanych, pełny projekt schematu, polityka prawna danych. Nie podejmuję decyzji — pokazuję opcje, koszty i rekomendację. Wszystkie tezy o licencjach i API pochodzą z linkowanych źródeł; miejsca niepotwierdzone oznaczam wprost.

## Opcje

### 1. Baza produkcyjna i self-host

**1A. Tylko PostgreSQL (w tym self-host jako kontener w compose).** Jeden dialekt, pełne wsparcie Spring Data JDBC (PostgreSQL jest na [liście dialektów](https://docs.spring.io/spring-data/relational/reference/jdbc/getting-started.html)), partycjonowanie zakresowe, [advisory locks](https://www.postgresql.org/docs/current/explicit-locking.html), `ON CONFLICT`, częściowe indeksy i bogaty SQL. Dla self-hostu PostgreSQL w `docker compose` nadal nie jest usługą zewnętrzną: to jeden bundlowany kontener, uruchamiany razem z aplikacją. Koszt: użytkownik musi mieć Docker/Podman albo instalować PostgreSQL lokalnie; nie ma trybu „jeden plik i JAR”.

**1B. PostgreSQL + wbudowana baza plikowa (HSQLDB, H2 albo SQLite).** Wariant z ADR 0001: duże instalacje i chmura na PostgreSQL, mały self-host na pliku. HSQLDB jest na liście dialektów Spring Data JDBC i ma [licencję BSD](https://hsqldb.org/web/hsqlLicense.html); H2 jest dual-licencjonowany [MPL 2.0/EPL 1.0](https://www.h2database.com/html/license.html) i również ma dialekt Spring Data JDBC; SQLite jest w [domenie publicznej](https://www.sqlite.org/copyright.html), ale **nie ma dialektu Spring Data JDBC** — każde zapytanie musiałoby iść przez `JdbcClient` albo własny dialekt. Koszt: wspólny podzbiór SQL (ADR wyklucza m.in. `ON CONFLICT`, `MERGE`, `ILIKE`, partycjonowanie, advisory locks, tablice, enumy i indeksy częściowe), dwa razy więcej testów i dwie ścieżki migracji. HSQLDB ma też limit domyślny: plik `.data` rośnie do 64 GB (więcej po zwiększeniu ustawienia), a kopiowanie plików zewnętrznym narzędziem podczas pracy bazy jest [wprost odradzane](https://hsqldb.org/doc/2.0/guide/management-chapt.html).

**1C. Tylko SQLite (jak domyślna baza Uptime Kuma).** Najprostsze pakowanie: jeden plik, zero serwera, idealne dla małego self-hostu. Ale: brak dialektu Spring Data JDBC, jeden writer (WAL poprawia odczyt, nie równoległy zapis), brak partycjonowania i bogatszych typów, a backup online wymaga API backupu lub `VACUUM INTO`, nie zwykłego `cp`. Przy 250 pomiarach/s z 5 000 checków i 3 lokalizacji zapis jest mały, ale współbieżność aplikacji Java i ręczne mapowanie całego SQL zwiększają kod.

**1D. H2 zamiast HSQLDB.** Podobny scenariusz do 1B: plik, tryb embedded, dialekt Spring Data JDBC, wsparcie Liquibase/Flyway. H2 jest jednak częściej traktowany jako baza deweloperska; nie znalazłem w źródłach dowodu na jego przewagę nad HSQLDB w produkcji self-host. To opcja do rozważenia tylko w ramach 1B.

### 2. Przechowywanie wyników pomiarów

**2A. Zwykłe tabele + preagregacja w aplikacji.** Surowe pomiary trzymamy krótko (24–48 h), a obok nich kubełki 5 min, 1 h i 1 dzień, liczone przez aplikację (najlepiej przez lidera). Wykresy 24 h–365 dni czytają kubełki, a szczegóły i „ostatnie zdarzenia” — surowe wiersze lub osobną tabelę zdarzeń. Zaleta: działa na PostgreSQL i na bazie plikowej, bez rozszerzeń i bez partycjonowania. Koszt: własny, idempotentny job agregujący; trzeba obsłużyć spóźnione dane, zmiany stref czasowych/DST i retencję. To najbardziej „KISS” wariant dla jednej osoby, jeśli nie chcemy wiązać się z PostgreSQL.

**2B. Partycjonowanie PostgreSQL.** Deklaratywne [partycjonowanie zakresowe](https://www.postgresql.org/docs/current/ddl-partitioning.html) po czasie; retencja przez `DROP TABLE`/`DETACH PARTITION` zamiast masowego `DELETE`, co unika `VACUUM` po wielkim kasowaniu. Dokumentacja podaje regułę kciuka: partycjonowanie ma sens, gdy tabela przekracza pamięć serwera; przy 5 000 checków i surowych danych to osiągalne w ciągu dni, nie miesięcy. Koszt: PostgreSQL-only, trzeba zarządzać tworzeniem/usuwaniem partycji (job lub `pg_partman` jako kolejna zależność) i pamiętać, że indeksy na tabeli partycjonowanej też są „wirtualne”.

**2C. TimescaleDB.** Rozszerzenie PostgreSQL: hypertabele, ciągłe agregaty i polityki retencji. Licencja jest dwuczłonowa: kod poza katalogiem `tsl` jest Apache 2.0, a kod w `tsl` podlega Timescale License (TSL). Community Edition jest darmowa do self-hostu i własnego użytku, ale nie wolno jej oferować jako DBaaS; ciągłe agregaty, polityki retencji, kompresja i `time_bucket_gapfill` są w edycji Community/TSL, nie w Apache-2. Koszt: rozszerzenie do instalowania i aktualizowania razem z PostgreSQL, wersjonowanie, lock-in na funkcje Timescale, brak wsparcia w HSQLDB. Dla 5 000 checków to technicznie wygodne, ale nadmiarowe, jeśli 2A wystarcza. Licencję i podział edycji opisuje [Tiger Data](https://www.tigerdata.com/docs/get-started/choose-your-path/timescaledb-editions).

**2D. Osobna baza szeregów czasowych (np. VictoriaMetrics).** Single-node [VictoriaMetrics](https://raw.githubusercontent.com/VictoriaMetrics/VictoriaMetrics/master/README.md) jest Apache 2.0, ma świetną kompresję i PromQL/MetricsQL, a wersja Enterprise dodaje m.in. downsampling i wiele retencji. Koszt: drugi datastore do wdrożenia, backupu, monitorowania i aktualizacji; dane pomiarowe przestają być transakcyjnie spójne z konfiguracją, incydentami i status page; self-host robi się cięższy. Sensowne dla dużej chmury, nie dla MVP i nie dla „self-host bez usług”.

#### Wolumen danych — jawne założenia i wyliczenia

Założenia: 5 000 checków, interwał 60 s, **3 lokalizacje** i każdy check mierzony z każdej lokalizacji; doba = 86 400 s, rok = 365 dni; GB = 10^9 B; surowy wiersz = 140 B (heap + indeks PK, bez WAL, TOAST i backupów); wiersz kubełka = 60 B (avg/min/max/p95/count/suma/statusy, bez próbek); nie liczę tabel konfiguracji, incydentów, zdarzeń, treści odpowiedzi, WAL i backupów.

| Warstwa | Wiersze/dobę | Wiersze/rok | B/wiersz | GB/dobę | GB/rok |
|---|---:|---:|---:|---:|---:|
| surowe 1 min | 21,6 mln | 7,884 mld | 140 | 3,0 | 1 104 |
| kubełki 5 min | 4,32 mln | 1,577 mld | 60 | 0,26 | 94,6 |
| kubełki 1 h | 360 tys. | 131,4 mln | 60 | 0,022 | 7,9 |
| kubełki 1 dzień | 15 tys. | 5,475 mln | 60 | 0,0009 | 0,33 |

Skalowanie: przy 1 lokalizacji podziel wartości przez 3 (surowe: 7,2 mln wierszy i 1,0 GB/dobę, 368 GB/rok); przy 5 lokalizacjach pomnóż przez 5/3 (surowe: 36 mln wierszy i 5,0 GB/dobę, 1,84 TB/rok). Przykładowa polityka retencji: surowe 24 h + 5 min przez 90 dni + 1 h przez 365 dni + 1 dzień przez 5 lat daje ok. 569 mln wierszy i **35,9 GB** netto (z 30% narzutem ok. 47 GB); sama surowa doba przez rok to ok. 1,1 TB. To jest główny argument za preagregacją: bez niej mały VPS nie utrzyma 365-dniowych wykresów.

### 3. Dostęp do bazy (JVM; krótko Go)

**3A. Sam `JdbcClient` (Spring Framework 7.1).** Jedna zależność mniej niż Spring Data JDBC, pełna kontrola nad SQL, wygodne mapowanie przez `RowMapper` ([dokumentacja](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html)). Brak repozytoriów i mapowania agregatów — dla części konfiguracyjnej trzeba pisać ręczne INSERT/UPDATE i mapowanie. Dla wyników pomiarów i raportów to naturalny wybór.

**3B. Spring Data JDBC + `JdbcClient` (stan z ADR 0001).** Repozytoria dla agregatów domenowych, jawny SQL bez ORM, `JdbcClient` dla batchy i raportów. Dokumentacja potwierdza dialekty PostgreSQL, HSQLDB, H2, MariaDB, MySQL, Oracle, DB2 i SQL Server — **SQLite nie ma**. To wciąż najlepszy kompromis „mało magii, mało kodu”.

**3C. jOOQ Open Source Edition.** [Apache 2.0, darmowe](https://www.jooq.org/download/), wspiera najnowsze wersje PostgreSQL, HSQLDB, H2, SQLite i innych baz open source. Generuje typowany DSL i kod z metadanych; płatne edycje Express (99 €/dev/rok), Professional (399 €) i Enterprise (799 €) dodają starsze wersje baz, bazy komercyjne i wsparcie. Koszt: plugin do codegenu, generowany kod w buildzie, sprzężenie z wersją bazy i jOOQ; dla jednej osoby to dodatkowy warsztat, który nie rozwiązuje problemu wolumenu.

**3D. JPA/Hibernate.** Pełny ORM z leniwym ładowaniem, cache, dirty checking i ukrytym SQL. Dla time-series i raportów to anty-wzorzec: trudno kontrolować batchowanie i plan zapytań, a mapowanie encji konfiguracyjnych nie jest warte ciężaru. Nie polecam.

**3E. MyBatis.** Apache 2.0, SQL w mapperach XML/annotacjach, dobra kontrola nad zapytaniami. Ale to kolejna warstwa i osobny styl, który nie integruje się z repozytoriami Spring Data tak naturalnie; dla jednej osoby mniej KISS niż 3B.

**Go (gdyby serwer był w Go).** [`database/sql`](https://pkg.go.dev/database/sql) to standardowa warstwa abstrakcji; dla PostgreSQL [`pgx` v5](https://raw.githubusercontent.com/jackc/pgx/master/README.md) jest czysto-goowym sterownikiem z obsługą `COPY`, `LISTEN/NOTIFY`, puli `pgxpool` i trybu `database/sql`. [`sqlc`](https://sqlc.dev/) generuje typowany kod z SQL. To dobre połączenie, ale wybór języka to osobna decyzja; przy Javie 3B wystarcza.

### 4. Migracje

**4A. Liquibase Community.** Licencja to **FSL-1.1-ALv2** (Functional Source License), a nie Apache 2.0: darmowe użycie poza „competing use”, po dwóch latach wersja przechodzi na Apache 2.0 ([README](https://raw.githubusercontent.com/liquibase/liquibase/master/README.md), [treść FSL](https://www.liquibase.com/liquibase-functional-source-license)). Liquibase Secure to [płatne plany](https://www.liquibase.com/pricing) (Starter/Team/Business/Enterprise, wycena). Zaleta: jeden changelog XML/YAML z changesetami `dbms`-specyficznymi, wsparcie HSQLDB, H2 i SQLite. Wada: FSL nie jest licencją OSI; dla projektu open source trzeba świadomie zaakceptować ten warunek. To bezpośrednia zmiana względem założenia ADR „Liquibase XML”.

**4B. Flyway OSS.** Rdzeń jest [Apache 2.0](https://github.com/flyway/flyway); Redgate Flyway Community jest darmowe, ale zawiera dodatki proprietary, a Teams/Enterprise są [płatne per użytkownik](https://documentation.red-gate.com/flyway/learn-more-about-flyway/commercial-licensing-faq). Flyway OSS wspiera PostgreSQL oraz HSQLDB przez [moduł `flyway-database-hsqldb`](https://documentation.red-gate.com/flyway/reference/database-driver-reference/hsqldb); obsługuje też H2 i SQLite (SQLite bez współbieżnych migracji). Zaleta: SQL-first, proste pliki `V1__...sql`, idealne przy jednym dialekcie i podejściu forward-only. Wada: przy dwóch dialektach trzeba by duplikować SQL albo pisać migracje w Javie; `undo` jest w wersjach płatnych.

**4C. Własny mały migrator.** Tabela `schema_version` + posortowane pliki SQL + transakcja. Ok. 150–250 linii, 2–3 dni. Trudne: checksumy, blokada przy wielu instancjach, częściowo zastosowane migracje, brak narzędzi do diagnostyki. Zgodnie z zasadą „rzeczy nietrywialne bierz z biblioteki” — nie polecam, chyba że zostajemy przy jednej instancji i prostym schemacie.

**4D. Liquibase Secure / Flyway Teams.** Płatne, dają wsparcie, polityki, rollback i dodatki enterprise. Dla jednej instancji i forward-only to zbędny koszt; wracamy do tego, gdy pojawi się wielu operatorów lub wymóg audytu.

### 5. Kopie zapasowe i odtwarzanie

**5A. PostgreSQL.** [`pg_dump`](https://www.postgresql.org/docs/current/app-pgdump.html) robi spójny eksport nawet przy ruchu i nie blokuje czytelników/pisarzy, ale dokumentacja wprost mówi, że poza prostymi przypadkami nie jest najlepszym wyborem do regularnych backupów produkcji. Dla małego self-hostu: dzienny `pg_dump -Fc` + rotacja + kopia poza VPS (RPO do 24 h). Dla chmury i mniejszego RPO: archiwizacja WAL/PITR u dostawcy zarządzanego albo `pg_basebackup` + WAL (RPO minuty). Trzeba testować odtworzenie, nie tylko backup.

**5B. HSQLDB.** Wbudowane [`BACKUP DATABASE TO ...`](https://hsqldb.org/doc/2.0/guide/management-chapt.html) (online, tar.gz) albo offline `DbBackupMain`. Dokumentacja odradza kopiowanie plików zewnętrznym programem, gdy baza działa. Limit `.data` 64 GB jest realnym sufitem dla strategii retencji.

**5C. SQLite.** [Online Backup API](https://www.sqlite.org/backup.html) przenosi spójny snapshot bez długiego blokowania; alternatywy to `VACUUM INTO` i `sqlite3_rsync`. Zwykłe `cp` działającej bazy jest ryzykowne. Brak wbudowanej retencji backupów — trzeba napisać rotację.

**5D. Chmura.** Zarządzany PostgreSQL zwykle daje PITR i backupy po stronie dostawcy; aplikacja powinna dodatkowo robić eksport logiczny i trzymać go w innym regionie. Product-features wymaga widocznego „ostatniego zrzutu” i „ostatniego testu odtworzenia”, a klucz szyfrowania sekretów ma być kopiowany osobno — to trzeba zaprojektować razem z backupem.

### 6. Blokada lidera (wiele instancji)

**6A. Tabela lease + fencing token (stan z ADR).** `leader_lock(name, holder_id, fencing_token, expires_at)`, atomowy `UPDATE ... WHERE`, czas z bazy, TTL 30 s i odnowienie co 10 s. Działa na PostgreSQL i HSQLDB, jest obserwowalna w SQL, a token fencing pozwala odrzucić spóźnione zadania. Koszt: jeden zapis co 10 s, poprawność zależy od zegara bazy i od tego, czy każde zadanie respektuje token.

**6B. Advisory locks PostgreSQL.** Sesja bierze lock, serwer zwalnia go automatycznie po zakończeniu sesji; nie ma wierszy do bloatowania i jest szybciej. Dokumentacja podaje jednak, że to locki o znaczeniu aplikacyjnym, więc aplikacja musi sama zadbać o poprawność. Wada: PostgreSQL-only, brak fencing tokenu, ryzyko przy poolowaniu sesji (lock trzymany przez połączenie z puli) i brak odpowiednika w HSQLDB.

**6C. Brak HA w MVP.** Jeden proces, jeden writer, bez blokady. Najmniej kodu i najmniej błędów, ale rolling deploy lub dwa procesy na chmurze mogą pisać równolegle. Jeśli MVP ma być single-instance, to akceptowalne; jeśli nie — 6A.

### Jak robią inni

| Projekt | Baza | Wyniki pomiarów | Retencja/agregacja | Migracje | Źródło |
|---|---|---|---|---|---|
| Uptime Kuma | SQLite domyślnie (better-sqlite3), opcja MariaDB | surowe `heartbeat` + w 2.x tabele `stat_minutely`, `stat_hourly`, `stat_daily` | usuwa nieistotne heartbeaty, zostawia 24 h albo 100 wierszy na monitor | knex migrations | [database.js](https://raw.githubusercontent.com/louislam/uptime-kuma/master/server/database.js) |
| OpenStatus | Turso/libSQL + Drizzle; Tinybird do analityki | pomiary i analityka w Tinybird; checker w Go | zarządzane przez Tinybird; szczegóły retencji niepotwierdzone | Drizzle; dokładny mechanizm migracji niepotwierdzony | [README](https://raw.githubusercontent.com/openstatusHQ/openstatus/main/README.md) |
| Gatus | `memory` domyślnie; opcjonalnie SQLite/PostgreSQL | wyniki per endpoint | max 100 wyników i 50 zdarzeń na endpoint; UI do 7 dni, dłużej przez własną agregację | brak udokumentowanych migracji; schemat tworzony przez aplikację | [README](https://raw.githubusercontent.com/TwiN/gatus/master/README.md) |
| Healthchecks | PostgreSQL/MySQL/MariaDB; SQLite w dev | pingi i zdarzenia | domyślnie 100 ostatnich pingów na check, limit konfigurowalny | Django `manage.py migrate` | [README](https://raw.githubusercontent.com/healthchecks/healthchecks/master/README.md) |
| Checkmate | MongoDB (wymagany) | `checks`, `monitorstats`, logi Docker | własne data migrations; backup `mongodump`/`mongorestore` | własne migracje danych | [README](https://raw.githubusercontent.com/bluewave-labs/Checkmate/develop/README.md) |
| Statping-ng | SQLite/MySQL/PostgreSQL | modele GORM | brak wyraźnej polityki w README | GORM `AutoMigrate` | [database.go](https://raw.githubusercontent.com/statping-ng/statping-ng/master/database/database.go) |
| Upptime | brak bazy; git + GitHub Issues | response time commitowany do repo co 6 h | historia w gicie, bez kasowania | brak migracji | [README](https://raw.githubusercontent.com/upptime/upptime/master/README.md) |

Wniosek z tabeli: projekt, który ma 365 dni wykresów, musi albo trzymać wąską tabelę surową i agregować (Kuma), albo oddać analitykę wyspecjalizowanemu systemowi (OpenStatus/Tinybird), albo ograniczyć horyzont (Gatus, Healthchecks). Żaden z badanych nie utrzymuje pełnej rozdzielczości rocznej w jednej tabeli na małym VPS.

## Porównanie

### Baza

| Kryterium | 1A PostgreSQL only | 1B PostgreSQL + plik | 1C SQLite only |
|---|---|---|---|
| Dialekty do utrzymania | 1 | 2 | 1 (ale bez dialektu Spring Data JDBC) |
| Partycjonowanie / advisory locks | tak | tylko na PG | nie |
| Self-host „jeden plik” | nie (kontener) | tak (HSQLDB/H2/SQLite) | tak |
| Limit skali pliku | brak | HSQLDB `.data` 64 GB domyślnie | praktyczny limit jednego writera |
| Koszt utrzymania | niski | wysoki (SQL subset, 2× CI, 2× migracje) | średni (ręczny SQL, brak dialektu) |
| Ryzyko na 10 lat | niskie | średnie (dwa sterowniki, dwie ścieżki) | średnie (zależność od xerial/sqlite) |

### Wyniki pomiarów

| Kryterium | 2A tabela + agregacja | 2B partycje PG | 2C TimescaleDB | 2D VictoriaMetrics |
|---|---|---|---|---|
| Przenośność | PG + HSQLDB/H2 | tylko PG | tylko PG + rozszerzenie | osobna usługa |
| Retencja | `DELETE`/batch + kubełki | `DROP`/`DETACH` partycji | polityki retencji | retencja VM |
| Złożoność | średnia (własny job) | średnia (job partycji) | wysoka (extension, wersje) | wysoka (drugi system) |
| Licencja | bez zmian | PostgreSQL | Apache 2 + TSL | Apache 2 (Enterprise płatne) |
| Self-host | najlepszy | dobry | cięższy | najcięższy |
| Ryzyko lock-in | niskie | średnie | wysokie | wysokie |

### Dostęp do bazy

| Kryterium | 3A JdbcClient | 3B Spring Data JDBC + JdbcClient | 3C jOOQ OSS | 3D JPA/Hibernate | 3E MyBatis |
|---|---|---|---|---|---|
| Licencja | Apache 2 (Spring) | Apache 2 (Spring) | Apache 2 (OSS) | [Apache 2](https://raw.githubusercontent.com/hibernate/hibernate-orm/main/LICENSE.txt) (Hibernate) | [Apache 2](https://raw.githubusercontent.com/mybatis/mybatis-3/master/LICENSE) |
| Sterowanie SQL | pełne | pełne | pełne, typowane | słabe (generowany SQL) | pełne |
| Codegen / build | nie | nie | tak | nie | nie |
| Wolumen/time-series | dobry | dobry | dobry | słaby | dobry |
| Krzywa dla jednej osoby | niska | niska | średnia | wysoka | średnia |
| Ryzyko na 10 lat | niskie | niskie | średnie (wersje baz) | średnie | niskie |

### Migracje

| Kryterium | 4A Liquibase Community | 4B Flyway OSS | 4C własny migrator |
|---|---|---|---|
| Licencja | FSL-1.1-ALv2 (po 2 latach Apache 2) | Apache 2.0 | własna |
| Format | XML/YAML/JSON/SQL | SQL + Java | SQL |
| Dwa dialekty | jeden changelog + `dbms` | duplikacja lub Java | ręcznie |
| Rollback | tak (zależnie od changesetu) | `undo` płatne | ręcznie |
| Wsparcie HSQLDB/H2/SQLite | tak | tak (HSQLDB przez moduł) | zależy |
| Koszt utrzymania | średni | niski (PG-only) | średni/wysoki |

### Blokada lidera

| Kryterium | 6A lease + fencing | 6B advisory lock PG | 6C brak HA |
|---|---|---|---|
| Przenośność | PG + HSQLDB | tylko PG | dowolna |
| Fencing | tak | nie | nie |
| Bloat / zapisy | wiersz co 10 s | brak | brak |
| Odporność na split-brain | dobra (TTL + token) | zależy od sesji | brak |
| Złożoność | średnia | niska | bardzo niska |

### Licencje — sprawdzone w źródłach

| Projekt | Wersja / edycja | Licencja | Co darmowe, co płatne |
|---|---|---|---|
| jOOQ | 3.21.10 | Open Source: Apache 2.0 | OSS darmowe dla najnowszych PG/HSQLDB/H2/SQLite; Express 99 €, Professional 399 €, Enterprise 799 € za stanowisko/rok |
| TimescaleDB | Community / Apache 2 | Apache 2.0 + TSL | self-host Community darmowy; zakaz oferowania jako DBaaS; część funkcji tylko w TSL |
| Liquibase | Community 5.x / Secure | FSL-1.1-ALv2 / komercyjna | Community darmowe poza „competing use”, po 2 latach Apache 2; Secure płatne |
| Flyway | OSS / Community / Teams / Enterprise | Apache 2.0 / proprietary / komercyjna | OSS Apache 2; Community darmowe; Teams i Enterprise płatne per użytkownik; `undo` w płatnych |

## Własna implementacja

- **Agregacja kubełków (2A):** ok. 300–500 linii + testy, 3–5 dni. Trudne: idempotencja przy ponownym uruchomieniu, spóźnione pomiary, DST i strefa przestrzeni, granice kubełków, zgodność sum z surowymi danymi. Test: property-based „suma kubełków = suma surowych” na wygenerowanym zbiorze, test granic doby i DST.
- **Retencja i partycje (2B):** ok. 100–200 linii, 2–3 dni. Trudne: tworzenie partycji z wyprzedzeniem, `DETACH`/`DROP` bez blokad, monitorowanie bloatu i zaległości. Test: retencja na małym zbiorze + sprawdzenie, że stare partycje znikają, a nowe się tworzą.
- **Wrapper backupu (5A/5B):** ok. 100–200 linii, 2–3 dni. Trudne: spójny snapshot, szyfrowanie, rotacja, weryfikacja odtworzenia i odszyfrowania sekretów. Test: backup → restore do pustej bazy → porównanie liczby wierszy i sum kontrolnych.
- **Lease lidera (6A):** ok. 150–250 linii, 2–3 dni. Trudne: zegar bazy, wygaśnięcie, fencing, zachowanie przy utracie połączenia. Test: dwóch kandydatów, jeden zdobywa lease; po TTL drugi przejmuje; spóźnione zadanie z starym tokenem jest odrzucane.
- **Czego nie pisać samemu:** sterownika bazy, puli połączeń, parsera SQL, narzędzia migracji, formatu backupu. To rzeczy wrażliwe albo trywialne do zepsucia; używamy PostgreSQL JDBC/HikariCP, Spring JDBC, Flyway/Liquibase i `pg_dump`/`BACKUP DATABASE`.

## Rekomendacja

1. **Baza:** PostgreSQL jako jedyna baza produkcyjna i self-host w bundlowanym `docker compose`. Jeden dialekt, partycjonowanie, advisory locks i pełne wsparcie Spring Data JDBC. HSQLDB/H2 tylko wtedy, gdy właściciel uzna „self-host bez kontenera DB” za twardy wymóg — wtedy trzeba zaakceptować wspólny podzbiór SQL, limit 64 GB i brak partycjonowania. SQLite odrzucam jako główną bazę: brak dialektu Spring Data JDBC i gorsza współbieżność.
2. **Wyniki:** zwykłe tabele + preagregacja w aplikacji (surowe 24–48 h, kubełki 5 min/1 h/1 dzień). Na PostgreSQL dodatkowo miesięczne partycje dla surowych danych i retencja przez `DROP`/`DETACH`. TimescaleDB i VictoriaMetrics dopiero przy skali chmury większej niż 5 000 checków.
3. **Dostęp:** Spring Data JDBC dla agregatów konfiguracyjnych + `JdbcClient` dla wyników, batchy i raportów. Bez jOOQ, JPA i MyBatis.
4. **Migracje:** Flyway OSS (Apache 2.0) z prostymi migracjami SQL, bo rekomendacja to PostgreSQL-only i forward-only. Liquibase Community rozważać tylko przy pozostaniu przy dwóch dialektach; jego FSL trzeba wtedy świadomie zaakceptować.
5. **Backup:** dzienny `pg_dump -Fc` + rotacja + kopia poza VPS; dla chmury PITR/WAL. Test odtworzenia co miesiąc, klucz szyfrowania sekretów kopiowany osobno. Przy HSQLDB: `BACKUP DATABASE` lub `DbBackup`.
6. **Lider:** tabela lease + fencing token (6A), bo działa na obu bazach i jawnie modeluje split-brain. Advisory locks tylko jako optymalizacja PG-only bez fencing.

**Łączna lista zależności rekomendacji:** PostgreSQL JDBC driver, HikariCP (przez Spring Boot), Spring Data JDBC, Spring JDBC (`JdbcClient`), Flyway OSS (+ moduł bazy dla PostgreSQL/HSQLDB, jeśli wymagany w wersji 10+), `pg_dump`/`pg_restore` jako narzędzia systemowe. Bez jOOQ, Hibernate, MyBatis, TimescaleDB, VictoriaMetrics, Redis i bez własnego migratora.

## Ryzyka

| Ryzyko | Mitygacja |
|---|---|
| „Self-host bez usług” zostanie odebrany jako wymagający PostgreSQL | Bundlowany compose, jedna komenda, healthcheck, dokumentacja; alternatywnie HSQLDB jako opcja, ale z jasnym limitem |
| Błąd agregacji fałszuje uptime | Przeliczanie ostatnich 24 h z surowych, testy sum i granic, idempotentne kubełki, alarm przy rozjeździe |
| Retencja przez `DELETE` bloatuje tabelę | Partycje miesięczne + `DROP`/`DETACH`, batch delete tylko jako fallback |
| Flyway OSS zmieni licencję w przyszłości | Pin wersji, plan B: Liquibase lub własny runner; dziś Apache 2.0 |
| FSL w Liquibase okaże się nieakceptowalny | Wybór Flyway OSS albo starszej wersji Apache 2; decyzja właściciela |
| HSQLDB przekroczy 64 GB lub nie utrzyma 250 zapisów/s | Benchmark na etapie 0, krótka retencja surowych, `SHUTDOWN COMPACT`, ścieżka migracji do PG |
| Lease bez fencing przepuści spóźnione zadanie | Token monotoniczny, sprawdzanie przy każdym zadaniu, TTL krótszy niż przerwa w odnowieniu |
| `pg_dump` daje RPO 24 h | WAL/PITR dla chmury, jawny RPO w dokumentacji, test odtworzenia |
| Utrata klucza szyfrowania sekretów | Osobna kopia klucza, test odtworzenia z odszyfrowaniem, rotacja bez przestoju |
| 5 000 checków przeciąży mały VPS | Batchowany zapis, pula Hikari niezależna od wątków, benchmark 5 000 checków na etapie 0/1 |

## Pytania do grill-me

1. Czy self-host może wymagać bundlowanego kontenera PostgreSQL, czy „bez usług zewnętrznych” oznacza też brak jakiegokolwiek kontenera bazy? To rozstrzyga 1A vs 1B.
2. Czy akceptujesz FSL-1.1-ALv2 dla Liquibase Community, czy wymagasz licencji OSI i wybierasz Flyway OSS?
3. Jaki jest wymagany RPO/RTO dla self-host i chmury — wystarczy dzienny `pg_dump` (RPO 24 h), czy potrzebne WAL/PITR (RPO minuty)?
4. Czy MVP ma działać jako jedna instancja, czy przewidujesz rolling deploy i wiele instancji od pierwszego wydania? To decyduje o 6A vs 6C.
5. Czy 365-dniowe wykresy mogą być z kubełków 1 h/1 dzień, czy wymagasz 5-minutowej rozdzielczości przez cały rok (94,6 GB vs 7,9 GB na rok)?
6. Czy zostawiamy HSQLDB jako „no-container” fallback, czy usuwamy ją z MVP i wspieramy tylko PostgreSQL?
7. Czy zapisujemy pełne request/response dla każdego checka, czy tylko dla błędów i zmian stanu? To wpływa na wolumen, GDPR i rozmiar backupu.
8. Czy chmura może użyć TimescaleDB albo VictoriaMetrics, czy chcesz jeden stack dla chmury i self-hostu?
9. Czy retencja ma być per plan (np. 7/30/365 dni) i czy stare dane mają być twardo usuwane, czy najpierw eksportowane?
10. Czy backup ma być szyfrowany po stronie aplikacji, gdzie trzymamy klucz i jak często wykonujemy test odtworzenia?

## Źródła

1. Uptime Kuma, README: https://raw.githubusercontent.com/louislam/uptime-kuma/master/README.md
2. Uptime Kuma, `server/database.js` (SQLite, MariaDB, knex, tabele `stat_*`): https://raw.githubusercontent.com/louislam/uptime-kuma/master/server/database.js
3. Gatus, README (storage memory/sqlite/postgres, limity wyników): https://raw.githubusercontent.com/TwiN/gatus/master/README.md
4. OpenStatus, README (Turso, Drizzle, Tinybird, Go checker): https://raw.githubusercontent.com/openstatusHQ/openstatus/main/README.md
5. Healthchecks, README (Django, PostgreSQL/MySQL/MariaDB, retencja pingów): https://raw.githubusercontent.com/healthchecks/healthchecks/master/README.md
6. Checkmate, README (MongoDB, mongodump/mongorestore, data migrations): https://raw.githubusercontent.com/bluewave-labs/Checkmate/develop/README.md
7. Statping-ng, README (SQLite/MySQL/PostgreSQL): https://raw.githubusercontent.com/statping-ng/statping-ng/master/README.md
8. Statping-ng, `database/database.go` (GORM AutoMigrate): https://raw.githubusercontent.com/statping-ng/statping-ng/master/database/database.go
9. Upptime, README (GitHub Actions, git, Issues): https://raw.githubusercontent.com/upptime/upptime/master/README.md
10. TimescaleDB, LICENSE (Apache 2 + TSL): https://raw.githubusercontent.com/timescale/timescaledb/main/LICENSE
11. Tiger Data, Timescale License: https://www.tigerdata.com/legal/licenses
12. Tiger Data, porównanie edycji: https://www.tigerdata.com/docs/get-started/choose-your-path/timescaledb-editions
13. jOOQ, wersje i ceny: https://www.jooq.org/download/
14. Liquibase, cennik Secure: https://www.liquibase.com/pricing
15. Liquibase, Community vs Secure: https://www.liquibase.com/community-vs-secure
16. Liquibase, README (FSL): https://raw.githubusercontent.com/liquibase/liquibase/master/README.md
17. Liquibase, FSL-1.1-ALv2: https://www.liquibase.com/liquibase-functional-source-license
18. Liquibase, wspierane bazy: https://www.liquibase.com/supported-databases
19. Flyway, licencjonowanie komercyjne: https://documentation.red-gate.com/flyway/learn-more-about-flyway/commercial-licensing-faq
20. Flyway, repozytorium OSS (Apache 2.0): https://github.com/flyway/flyway
21. Flyway, wspierane bazy: https://documentation.red-gate.com/flyway/getting-started-with-flyway/system-requirements/supported-databases-and-versions
22. Flyway, HSQLDB: https://documentation.red-gate.com/flyway/reference/database-driver-reference/hsqldb
23. PostgreSQL, partycjonowanie: https://www.postgresql.org/docs/current/ddl-partitioning.html
24. PostgreSQL, advisory locks: https://www.postgresql.org/docs/current/explicit-locking.html
25. PostgreSQL, `pg_dump`: https://www.postgresql.org/docs/current/app-pgdump.html
26. HSQLDB, zarządzanie i backup (limit `.data` 64 GB): https://hsqldb.org/doc/2.0/guide/management-chapt.html
27. HSQLDB, licencja BSD: https://hsqldb.org/web/hsqlLicense.html
28. SQLite, online backup API: https://www.sqlite.org/backup.html
29. SQLite, domena publiczna: https://www.sqlite.org/copyright.html
30. H2, licencja MPL 2.0/EPL 1.0: https://www.h2database.com/html/license.html
31. VictoriaMetrics, README (Apache 2, Enterprise): https://raw.githubusercontent.com/VictoriaMetrics/VictoriaMetrics/master/README.md
32. Spring Data JDBC, wymagania i dialekty: https://docs.spring.io/spring-data/relational/reference/jdbc/getting-started.html
33. Spring Framework, `JdbcClient`: https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html
34. pgx, README (Go, PostgreSQL): https://raw.githubusercontent.com/jackc/pgx/master/README.md
35. Hibernate ORM, LICENSE (Apache 2.0): https://raw.githubusercontent.com/hibernate/hibernate-orm/main/LICENSE.txt
36. MyBatis 3, LICENSE (Apache 2.0): https://raw.githubusercontent.com/mybatis/mybatis-3/master/LICENSE
37. Go, `database/sql`: https://pkg.go.dev/database/sql
38. sqlc, generowanie kodu z SQL: https://sqlc.dev/
