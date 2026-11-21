# 0005 — Architektura serwera: moduły, granice, porty i komunikacja

- Status: accepted
- Date: 2026-11-21
- Superseded-by: none

## Context

Serwer to jedna aplikacja Spring Boot z rolami (API, executor, alerty, agregacje), które mają dać się uruchomić jako osobne procesy samą konfiguracją ([0002-stack.md](0002-stack.md)). Właściciel chce KISS, SOLID, DRY i AHA, niezależności od konkretnych implementacji tam, gdzie się opłaca, oraz jak najwięcej reguł pilnowanych statycznie, bo kod w dużej części pisze AI, a AI zapomina o zasadach. Ogólne zasady backendu (DTO jako rekordy, walidacja na granicach, lekka architektura heksagonalna tylko przy realnej logice) pochodzą z `ai/domains/backend/AGENTS.md`; ten ADR dodaje decyzje lokalne. Decyzje zapadły w grill-me 2026-11-21.

Jak robią inni: Uptime Kuma i Checkmate dzielą kod na warstwy techniczne; Healthchecks (aplikacje Django) i Gatus (pakiety Go) dzielą według funkcji. Nikt nie stosuje pełnej architektury heksagonalnej, ale wszyscy mają jeden interfejs dla otwartych zbiorów (typy monitorów, kanały powiadomień). Nikt nie wymusza granic modułów testem; Gatus dostaje część za darmo z `internal/` w Go.

## Decision

### Moduły
- Kod dzielimy według funkcji (package-by-feature): `<root>.checks`, `<root>.results`, `<root>.alerts`, `<root>.incidents`, `<root>.statuspages`, `<root>.agents`, `<root>.verification`, `<root>.users`, `<root>.configfile`, `<root>.audit` itd. Bez globalnych `controller/`, `service/`, `repository/` i bez worków `common`/`shared`/`util`.
- **Publiczny jest tylko pakiet główny modułu; każdy podpakiet jest prywatny** (konwencja zgodna ze Spring Modulith, więc ewentualne przejście nic nie kosztuje).
- Warstwy `domain`/`application`/`adapters` dokładamy tylko w module z realną logiką. CRUD zostaje płaski.

### Pilnowanie granic
- **ArchUnit** (test JUnit, bez Spring Modulith) pilnuje:
  - braku cykli między modułami;
  - dostępu do innego modułu tylko przez jego pakiet główny;
  - jawnej mapy dozwolonych zależności między modułami i między rolami. Nowa krawędź to świadoma zmiana widoczna w diffie testu;
  - zasad z ADR: typy Reactora tylko w executorze ([0003](0003-network-checks.md)); zakaz `java.net.http`; czas tylko przez wstrzyknięty `java.time.Clock` (zakaz `Instant.now()`, `LocalDateTime.now()` itp.); pakiety `..domain..` bez `org.springframework..`.
- Każdą nową zasadę z ADR, którą da się sprawdzić statycznie, dopisujemy jako regułę ArchUnit. Reguły kodu (formatowanie, nulle, złożoność) opisuje [0007-static-checks.md](0007-static-checks.md).

### Porty (interfejsy)
- Reguła: port tylko wtedy, gdy już teraz są co najmniej dwie implementacje albo test logiki wymaga izolacji.
- Porty od startu:
  - `CheckExecutor`, jeden na typ checka, zbierany w rejestr beanów po typie. Nowy typ to nowa klasa, bez `switch`. Ta sama klasa dostarcza deskryptor formularza dla frontendu ([0004](0004-frontend.md)).
  - `NotificationChannel`, jeden na kanał (email, webhook, Pushover, później kolejne).
  - `LeaderLease`: implementacja na bazie ([0002](0002-stack.md)) i podróbka w testach.
  - Port kolejki między rolami (niżej).
  - `RateLimiter` ([0006](0006-api-contract.md)).
- Zegar: `java.time.Clock` z JDK jako bean, bez własnego interfejsu.
- Magazyn plików (archiwum, zrzuty): port dopiero na etapie 2, gdy pojawi się archiwum (dysk lokalny i S3).
- Bez portów nad bazą. Spring Data JDBC i wspólny podzbiór SQL już pełnią tę rolę.

### Role i komunikacja między modułami
- **Rola włącza punkty wejścia** (harmonogramy, nasłuchy, kontrolery). Kod wszystkich modułów jest w każdym procesie. „Sprawdź teraz” i „Testuj” API wykonuje synchronicznie, wołając moduł executora jak bibliotekę.
- W obrębie roli moduły wołają się bezpośrednio przez publiczny pakiet główny.
- **Między rolami** komunikacja idzie przez **jeden ogólny port o semantyce kolejki**, od pierwszego dnia, także w MVP z jednym procesem:
  - operacje `publish(topic, key, message)` oraz `poll(consumer, topic, limit)` → `ack(cursor)`;
  - dostarczenie co najmniej raz, konsumenci idempotentni, kolejność gwarantowana tylko w obrębie klucza (np. ID checka);
  - wiadomość jest samowystarczalna: wersjonowany JSON z ID i potrzebnymi danymi. Konsument nie czyta tabel producenta jako części przekazania;
  - producent zapisuje wiadomość w tej samej transakcji co zmianę stanu (transakcyjny outbox).
- Pierwszy adapter to tabela w bazie. Konsument odpytuje „wszystko po kursorze” mniej więcej co sekundę. Bez `LISTEN/NOTIFY`, bo HSQLDB go nie ma, i bez brokera.
- Jeden test kontraktowy portu uruchamiamy na każdej implementacji: dziś na PostgreSQL i HSQLDB, później na Redisie albo innej kolejce. Wymiana na zewnętrzną kolejkę to nowy adapter plus przekaźnik z outboxa. Kod producentów się nie zmienia.
- ArchUnit pilnuje, że moduł jednej roli nie woła bezpośrednio serwisu modułu innej roli.

### Model domeny
- Domyślnie jeden model: agregaty Spring Data JDBC to rekordy z `@Id`.
- Polityki (ocena stanu, kworum, flapping, okna serwisowe, różnice plik–UI) to czysta Java w pakiecie `domain` bez Springa, działająca na prostych rekordach i testowana jednostkowo.
- Wyniki pomiarów obsługuje `JdbcClient` z ręcznym `RowMapper`.
- DTO API są zawsze osobnymi rekordami. Mapowanie piszemy ręcznie, bez MapStruct.

## Consequences

- Port kolejki to świadomy wyjątek od reguły portów: wymiana na Redis albo inną kolejkę jest znaną ścieżką rozwoju, a koszt portu jest mały.
- Opóźnienie przekazania między rolami wynosi do ok. 1 s, a odpytywanie lekko obciąża bazę. W zamian rozdzielenie ról nie wymaga przepisywania przepływów.
- Bez Spring Modulith tracimy generowane diagramy modułów, `@ApplicationModuleTest` i rejestr zdarzeń. Zdarzenia Modulitha działają w jednym procesie, więc i tak nie pasowały do ról w osobnych procesach.
- Regułę modułów w ArchUnit piszemy sami (ok. 50–80 linii testu).
- Harmonogram executora: przy 5 000 checków wystarcza `ScheduledExecutorService`. Koło czasowe (`HashedWheelTimer` z `netty-common`, już na classpath) rozważamy tylko wtedy, gdy spike na etapie 0 pokaże problem.
- Nowa zależność: ArchUnit (tylko w testach).

## Links

- [0002-stack.md](0002-stack.md), [0003-network-checks.md](0003-network-checks.md), [0006-api-contract.md](0006-api-contract.md), [0007-static-checks.md](0007-static-checks.md)
