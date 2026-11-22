# 0005 — Architektura serwera: moduły, granice, porty i komunikacja

- Status: accepted
- Date: 2026-11-21
- Superseded-by: none

## Context

Serwer to jedna aplikacja Spring Boot z rolami (API, executor, alerty, agregacje), które mają dać się uruchomić jako osobne procesy samą konfiguracją ([0002-stack.md](0002-stack.md)). Właściciel chce KISS, SOLID, DRY i AHA, niezależności od konkretnych implementacji tam, gdzie się opłaca, oraz jak najwięcej reguł pilnowanych statycznie, bo kod w dużej części pisze AI, a AI zapomina o zasadach. Ogólne zasady backendu (DTO jako rekordy, walidacja na granicach, lekka architektura heksagonalna tylko przy realnej logice) pochodzą z ogólnych standardów właściciela (backend/frontend); ten ADR dodaje decyzje lokalne. Decyzje zapadły w grill-me 2026-11-21.

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
  - operacje `publish(topic, key, message)`, `poll(consumer, topic, limit)` oraz `ack(consumer, topic, cursor)`;
  - dostarczenie co najmniej raz, konsumenci idempotentni, kolejność gwarantowana tylko w obrębie klucza (np. ID checka);
  - wiadomość jest samowystarczalna: wersjonowany JSON z ID i potrzebnymi danymi. Konsument nie czyta tabel producenta jako części przekazania;
  - producent zapisuje wiadomość w tej samej transakcji co zmianę stanu (transakcyjny outbox).
- Pierwszy adapter to tabela w bazie. Konsument odpytuje „wszystko po kursorze” mniej więcej co sekundę. Bez `LISTEN/NOTIFY`, bo HSQLDB go nie ma, i bez brokera.
- Jeden test kontraktowy portu uruchamiamy na każdej implementacji: dziś na PostgreSQL i HSQLDB, później na Redisie albo innej kolejce. Wymiana na zewnętrzną kolejkę to nowy adapter plus przekaźnik z outboxa. Kod producentów się nie zmienia.
- ArchUnit pilnuje, że moduł jednej roli nie woła bezpośrednio serwisu modułu innej roli.

**Dlaczego publikujący czekają na siebie nawzajem.** Każda publikacja najpierw przesuwa wspólny licznik w bazie i trzyma jego wiersz zablokowany do końca transakcji, więc druga publikacja nie dostanie kursora, dopóki pierwsza się nie zatwierdzi. Dzięki temu konsument nie odczyta późniejszej wiadomości i nie przesunie kursora ponad wcześniejszą, jeszcze niezatwierdzoną. To serializuje publikację — przy obecnej skali to nie problem, a gdy stanie się wąskim gardłem, rozwiązaniem jest zewnętrzna kolejka jako nowy adapter.

`publish` wołamy jako ostatni krok transakcji producenta, bo blokada licznika trwa od `publish` do zatwierdzenia — długa transakcja z `publish` na początku wstrzymuje innych publikujących.

Pierwszy `ack` danego konsumenta na danym temacie wykonany jednocześnie z dwóch procesów (np. przy zmianie lidera) może w PostgreSQL skończyć się błędem duplikatu i wycofaniem tej jednej transakcji. Nic nie ginie, bo dostarczanie jest „co najmniej raz” i wiadomość wróci przy następnym `poll`.

**Gdy aplikacja zniknie bez zamknięcia połączenia.** Maszyna z aplikacją może zniknąć (awaria, zerwanie sieci) bez zamknięcia połączenia z bazą. PostgreSQL może wtedy długo nie zauważyć martwego klienta — domyślny keepalive TCP systemu to ok. 2 h — i przez ten czas trzymać blokadę wiersza licznika kolejki. Zabezpieczenia:
- `idle_in_transaction_session_timeout` (np. `60s`): serwer zamyka sesję, która siedzi bezczynnie w otwartej transakcji, więc blokada znika.
- `tcp_keepalives_idle`, `tcp_keepalives_interval`, `tcp_keepalives_count` po stronie serwera PostgreSQL: krótsze wykrywanie martwego połączenia TCP (0, domyślnie, zostawia ustawienia systemu).
- `lock_timeout` dla publikujących (np. `5s`): publikacja kończy się błędem i ponowieniem zamiast czekać bez końca na wiersz licznika.

Te parametry ustawia się w `postgresql.conf` (globalnie), przez `ALTER DATABASE ... SET` / `ALTER ROLE ... SET` (dla bazy albo roli aplikacji) lub w parametrach połączenia JDBC: pgjdbc przekazuje je do serwera przez `options`, np. `options=-c idle_in_transaction_session_timeout=60s -c lock_timeout=5s` (w URL wartość trzeba zakodować). `lock_timeout` lepiej ustawić dla roli aplikacji albo w połączeniu niż globalnie, bo dotyczy tylko publikujących. W tym zadaniu nie zmieniamy konfiguracji aplikacji.

Gotowe blokady (ShedLock, `JdbcLockRegistry`) opierają się na czasie (TTL) i nie są związane z commitem, a licznik kolejki musi zwalniać blokadę dokładnie przy `COMMIT`/`ROLLBACK`.

**Co monitorować w kolejce.** Kolejkę obserwujemy jak każdą kolejkę: czy producenci nie czekają za długo i czy konsumenci nadążają. Metryki przez Micrometer ([0002](0002-stack.md)):
- czas czekania publikującego na wiersz licznika (timer, p99). Trwale ponad ok. 100 ms albo pojedyncze czekanie ponad 0,5 s (log WARN) znaczy, że serializacja publikacji staje się wąskim gardłem;
- opóźnienie konsumenta: ostatni kursor minus kursor konsumenta, osobno dla konsumenta i tematu (gauge). Lag, który rośnie zamiast wracać do zera, znaczy, że konsument nie nadąża albo stoi.

Przy diagnozie na PostgreSQL pomaga `log_lock_waits = on` (log każdego czekania na blokadę dłuższego niż `deadlock_timeout`) i `pg_stat_activity` z `wait_event_type = 'Lock'`. Gdy oba sygnały rosną mimo krótkich transakcji producentów, czas na zewnętrzną kolejkę jako nowy adapter. Metryki wchodzą razem z pierwszym konsumentem w etapie 1; ich eksport do OTLP w etapie 8 ([roadmap.md](../specs/roadmap.md)).

### Model domeny
- Domyślnie jeden model: agregaty Spring Data JDBC to rekordy z `@Id`.
- Polityki (ocena stanu, kworum, flapping, okna serwisowe, różnice plik–UI) to czysta Java w pakiecie `domain` bez Springa, działająca na prostych rekordach i testowana jednostkowo.
- Wyniki pomiarów obsługuje `JdbcClient` z ręcznym `RowMapper`.
- DTO API są zawsze osobnymi rekordami. Mapowanie piszemy ręcznie, bez MapStruct.

### W prostych słowach: lider i lease

**Po co?** Serwer wakey może kiedyś działać jako kilka kopii naraz (np. A, B, C). Część pracy ma robić tylko jedna z nich: odpalanie checków według harmonogramu, alerty, agregacje. Inaczej każdy alert przyszedłby trzy razy.

**Lease to klucz do pokoju.** W bazie jest tabela `leader_lock`.
- Kopia A bierze klucz na 30 s. Dopóki go ma, jest liderem i robi pracę w tle.
- Lider co jakiś czas przedłuża klucz („wciąż żyję”).
- Gdy A padnie i nie przedłuży, po 30 s klucz wygasa i bierze go kopia B.
- Przy każdej zmianie właściciela rośnie numer — fencing token. A miał 1, B dostaje 2. Gdy A się „obudzi” i spróbuje coś zrobić ze starym numerem, da się go odrzucić, bo aktualny jest 2. To chroni przed dwoma liderami naraz.
- Czas bierzemy z bazy, nie z zegara procesu, bo zegary różnych maszyn mogą się rozjechać.

**Co się dzieli między kopie, a co nie:**

| Rodzaj pracy | Dzielony między kopie? |
|---|---|
| Ruch HTTP: API, panel, wyniki od agentów | Tak — load balancer kieruje zapytania do dowolnej kopii; stan (sesje, dane) jest w bazie |
| Harmonogram, alerty, agregacje | Nie — robi to tylko lider; reszta czeka w rezerwie, gotowa przejąć |
| Kolejka między rolami | Nie dzieli obciążenia — pozwala uruchomić role (executor, alerty) jako osobne procesy |

Konkurujący konsumenci (kilka kopii dzielących się pracą w tle) przyjdą dopiero, gdy jeden lider nie wyrobi ([0002](0002-stack.md)). W MVP działa jedna instancja; to wszystko jest gotowe na kolejne kopie bez przepisywania kodu.

### Logowanie (2026-11-22)

Logujemy dużo, żeby z logów było widać, co robi aplikacja — także na produkcji. Poziomy:
- **INFO** — zmiany stanu i zdarzenia ważne na produkcji: start i stop ról, przejęcie lub utrata lidera, wysłany alert.
- **DEBUG** — rutynowe kroki przydatne przy debugowaniu: każda próba, odnowienie lease, pominięcie.
- **WARN** — sytuacje nieoczekiwane, ale obsłużone.
- **ERROR** — błąd, który wymaga uwagi.

Zawsze SLF4J przez `@Slf4j`, placeholdery `{}`, wyjątek jako ostatni argument. Nigdy nie logujemy sekretów, tokenów ani haseł.

### Testy integracyjne (2026-11-22)

Testy integracyjne korzystają z jednego współdzielonego kontenera i jednego kontekstu Springa na bazę. Wzorzec to singleton container: statyczne pole z kontenerem w abstrakcyjnej klasie bazowej (`dev.wakey.testsupport`), start w bloku statycznym i `@ServiceConnection`; bez `@Testcontainers`/`@Container`, bo ich cykl życia zatrzymuje współdzielony kontener po pierwszej klasie testowej.

## Consequences

- Port kolejki to świadomy wyjątek od reguły portów: wymiana na Redis albo inną kolejkę jest znaną ścieżką rozwoju, a koszt portu jest mały.
- Opóźnienie przekazania między rolami wynosi do ok. 1 s, a odpytywanie lekko obciąża bazę. W zamian rozdzielenie ról nie wymaga przepisywania przepływów.
- Bez Spring Modulith tracimy generowane diagramy modułów, `@ApplicationModuleTest` i rejestr zdarzeń. Zdarzenia Modulitha działają w jednym procesie, więc i tak nie pasowały do ról w osobnych procesach.
- Regułę modułów w ArchUnit piszemy sami (ok. 50–80 linii testu).
- Harmonogram executora: przy 5 000 checków wystarcza `ScheduledExecutorService`. Koło czasowe (`HashedWheelTimer` z `netty-common`, już na classpath) rozważamy tylko wtedy, gdy problem pokażą testy albo pomiary na działającej instancji.
- Nowa zależność: ArchUnit (tylko w testach).

## Links

- [0002-stack.md](0002-stack.md), [0003-network-checks.md](0003-network-checks.md), [0006-api-contract.md](0006-api-contract.md), [0007-static-checks.md](0007-static-checks.md)
