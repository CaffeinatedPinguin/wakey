# Research: język i framework serwera wakey (v1)

## Zakres

Porównuję stack serwera wakey dla wymagań z `docs/specs/product-features.md` (sekcje 0, 1, 2, 3, 12): ~5 000 checków na VPS 1–2 vCPU i 2–4 GB RAM, praca I/O-bound, pełne API + OIDC + szyfrowanie sekretów + role w jednym procesie, self-host bez usług zewnętrznych (jeden obraz albo binarka), React osobno, agent w Go, projekt open source utrzymywany latami przez jedną osobę. Porównuję: Java 25 + Spring Boot 4 MVC na wątkach wirtualnych; Spring Boot jako „wrapper” z asynchronicznym transportem i klientem checków (WebFlux/Reactor Netty, Jetty 12, wariant mieszany); Quarkus i Micronaut (JVM i native); Kotlin + Spring Boot (korutyny); Go dla całego serwera (stdlib `net/http` + mały router); TypeScript/Node; krótko .NET i Rust. Nie badam: wyboru bibliotek frontendu, dostawców alertów, szczegółów schematu bazy ani finalnej decyzji — to należą do innych punktów backlogu i grill-me. Dane na 2026-11-20. Liczby bez źródła i warunków pomiaru oznaczam jako „niepotwierdzone”.

## Opcje

Punkt wyjścia: 5 000 checków przy interwale 60 s to średnio ~83 checki/s; przy 300 s ~17/s. To nie jest problem przepustowości HTTP dla żadnej z opcji. Różnicują: pamięć bazowa, złożoność operacyjna, ekosystem OIDC/migracji/testów, liczba toolchainów i wspólny kod z agentem.

### 1. Java 25 LTS + Spring Boot 4 MVC (wątki wirtualne)

- **Wersje i wsparcie:** Java 25 LTS wydana 2025-09-16, wsparcie Oracle do 2030-09-30; Java 27 (2026-09-15) nie jest LTS; kolejne LTS w cyklu dwuletnim ([endoflife.date/oracle-jdk](https://endoflife.date/oracle-jdk)). Spring Boot 4.2.0 wydany 2026-11-19, 4.1.3 i 4.0.10 — 2026-11-19; OSS 4.0 kończy się 2026-12-31, 4.1 — 2027-07-31, 3.5 skończył się 2026-06-30 ([endoflife.date/spring-boot](https://endoflife.date/spring-boot), [release 4.2.0](https://github.com/spring-projects/spring-boot/releases/tag/v4.2.0)). Boot 4.1 obsługuje Java 17–26. Boot 4 to duży major: Spring Framework 7, Jackson 3, nowe wersje Jakarta ([release notes 4.0](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)).
- **Model współbieżności:** `spring.threads.virtual.enabled=true` daje `SimpleAsyncTaskExecutor` i scheduler na wątkach wirtualnych; MVC, JDBC i blokujący klient HTTP działają bez przepisywania na reaktywne ([dokumentacja Spring Boot](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html)). Wątków wirtualnych nie pooluje się — do limitów używa się semaforów; JDBC/Hikari nadal limituje bazę ([JEP 444](https://openjdk.org/jeps/444)). Pinning w `synchronized` usunięto w JEP 491 (Java 24), więc Java 25 jest pod tym względem bezpieczniejsza niż 21 ([JEP 491](https://openjdk.org/jeps/491)).
- **Pamięć i start:** niezależny pomiar Java 25, Spring Boot 3.5.6, JDBC + Flyway + PostgreSQL, jedna tabela, JSON + HTML: start JVM 1,909 s, max RSS 388,9 MB; native 0,104 s i 149,4 MB ([Gillius, 2025-10-09](https://gillius.org/blog/2025/10/java-25-framework-startup.html)). NpgsqlRest (2026-07, Spring Boot 4.1.0 + Java 25 + virtual threads, PostgreSQL, 6 endpointów, równe rdzenie, 100 VU w scenariuszu perf-test): peak 756 MB, średnio 404 MB, CPU 98% z budżetu 400% ([NpgsqlRest](https://npgsqlrest.github.io/blog/benchmarks-2026-07/spring-boot.html)). Inny pomiar: Spring Boot 4.1.1 JVM 6,318 s i 229,2 MB, native 0,132 s i 79,5 MB — inne warunki, nie porównywać wprost ([ankurm, 2026-10-08](https://ankurm.com/spring-boot-4-1-vs-quarkus-vs-micronaut-benchmarked/)).
- **Obraz:** JAR + JRE; dokładny rozmiar obrazu **niepotwierdzony**. Native image zmniejsza RSS, ale wydłuża build i wymaga konfiguracji refleksji.
- **Ekosystem:** Spring Security OIDC, Liquibase/Flyway, springdoc, Testcontainers, Spring Data JDBC + HikariCP, Actuator; największa społeczność JVM ([Spring Security OIDC](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html), [springdoc](https://springdoc.org/), [Testcontainers](https://testcontainers.com/), [HikariCP](https://github.com/brettwooldridge/HikariCP)).
- **Wspólny kod z agentem:** brak; semantykę checków trzeba utrzymywać równolegle w Javie i Go, chronioną kontraktem JSON i testami parity.
- **Koszt utrzymania:** minor co ~6 miesięcy, OSS ~13 miesięcy; trzeba aktualizować co 6–12 miesięcy i pilnować zgodności springdoc/Liquibase/Testcontainers/Data JDBC. 4.0 wygasa 2026-12-31, więc pin musi być na 4.1.x albo 4.2.x.

### 2. Spring Boot jako „wrapper” + asynchroniczny transport (WebFlux, Jetty, wariant mieszany)

**Wspólne ramy.** Spring Boot 4.2.0 (2026-11-19) na Java 25 daje konfigurację, DI, Spring Security/OIDC, Actuator, Liquibase i testy niezależnie od wybranego serwera; 4.0 i 4.1 są tą samą rodziną, a OSS 4.0 kończy się 2026-12-31. Dokumentacja 4.2 potwierdza trzy wspierane serwery: Tomcat (domyślny), Jetty i Reactor Netty; Undertow został usunięty w 4.0, bo nie spełnia Servlet 6.1 ([Embedded Web Servers](https://docs.spring.io/spring-boot/how-to/webserver.html), [migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)). Wrapper nie zmienia więc ekosystemu OIDC/Actuator — zmienia model wykonania i klienta checków.

#### 2.1 Pełny async: WebFlux na Reactor Netty

- **Wersje:** Boot 4.2.0, Reactor Netty 1.4.0 (Netty 4.2), Java 25 ([Reactor Netty HTTP client](https://projectreactor.io/docs/netty/release/reference/http-client.html), [Boot reactive](https://docs.spring.io/spring-boot/reference/web/reactive.html)).
- **Model:** pętla zdarzeń Netty; domyślnie liczba workerów = liczba procesorów (minimum 4), wspólna dla serwera i klientów w JVM; blokowanie w pętli zatrzymuje wszystkie żądania na tym wątku ([event loop](https://projectreactor.io/docs/netty/release/reference/http-client.html)).
- **Klient checków:** `reactor.netty.http.client.HttpClient` daje zdarzenia per żądanie przez `doOnResolve`/`doAfterResolve` (DNS), `doOnConnect`/`doOnConnected` (TCP/TLS), `doOnRequest`, `doOnResponse`/`doAfterResponseSuccess` (nagłówki/TTFB i koniec) — tabela lifecycle callbacks ([HTTP client](https://projectreactor.io/docs/netty/release/reference/http-client.html)). `ConnectionObserver`/`ConnectionObserver.State` (`CONNECTED`, `ACQUIRED`, `RELEASED`, `DISCONNECTING`) nadaje się do wspólnego pomiaru cyklu połączenia ([ConnectionObserver](https://projectreactor.io/docs/netty/release/api/reactor/netty/ConnectionObserver.html)); `WebClient` opakowuje ten klient, ale ukrywa część zdarzeń.
- **Fazy i metryki:** Micrometer dostaje timery `address.resolver`, `connect.time`, `tls.handshake.time`, `response.time`, `data.*` — to agregaty (count/total/max), nie oś czasu pojedynczego żądania; pojedyncze fazy trzeba złożyć z callbacków ([metrics](https://projectreactor.io/docs/netty/release/reference/http-client.html), [Micrometer](https://docs.micrometer.io/micrometer/reference/concepts/meters.html)).
- **Resolver i przypięcie IP:** `resolver(...)` przyjmuje `AddressResolverGroup`; domyślnie Netty `DnsNameResolver` rozwiązuje asynchronicznie, a `queryTimeout`, `cacheMinTimeToLive`/`cacheMaxTimeToLive` i `resolvedAddressTypes` są konfigurowalne; własny `dnsAddressResolverGroupProvider` pozwala wstawić builder resolvera ([host name resolution](https://projectreactor.io/docs/netty/release/reference/http-client.html)). Netty `DnsNameResolver` obsługuje A/AAAA przez `ResolvedAddressTypes`, a TXT przez `resolveAll(new DnsQuestion(host, DnsRecordType.TXT))`; TTL jest w `DnsCache`/konfiguracji cache ([DnsNameResolver](https://netty.io/4.2/api/io/netty/resolver/dns/DnsNameResolver.html), [DnsRecordType](https://netty.io/4.2/api/io/netty/handler/codec/dns/DnsRecordType.html)). Przypięcie konkretnego IP przy zachowaniu SNI/Host wymaga własnego resolvera — mechanizm **niepotwierdzony** bez spike'u.
- **Koszt:** brak blokowania, mało wątków, ale cały serwer i klient są reaktywne; JDBC wymaga `boundedElastic` albo R2DBC. Dla 5 000 checków zysk nad VT jest **niepotwierdzony**; ryzyko to blokada event loop i trudniejszy debug.

#### 2.2 Spring Boot + Jetty 12 (serwer) i Jetty `HttpClient` (checki)

- **Wersje:** Boot 4.2.0 wspiera Jetty 12.1 na stosie servlet i reactive ([Embedded Web Servers](https://docs.spring.io/spring-boot/how-to/webserver.html)); Jetty `HttpClient` 12.1.15 jest non-blocking/asynchroniczny i ma osobną dokumentację klienta ([Jetty HTTP client](https://jetty.org/docs/jetty/12.1/programming-guide/client/http.html)).
- **Fazy:** listenery `Request.Listener` (queued, begin, headers, commit, success/failure) i `Response.Listener` (begin, headers, content, success/failure/complete); `Response.BeginListener.onBegin` to „best approximation to detect when the first bytes of the response arrived” ([Response.Listener](https://javadoc.jetty.org/jetty-12.1/org/eclipse/jetty/client/Response.Listener.html)). Dokumentacja wprost mówi, że pozwala „accurately time every step” ([Jetty HTTP client](https://jetty.org/docs/jetty/12.1/programming-guide/client/http.html)).
- **Połączenie i resolver:** `Connection.Listener.onOpened/onClosed` mierzy cykl połączenia ([Connection.Listener](https://javadoc.jetty.org/jetty-12/org/eclipse/jetty/io/Connection.Listener.html)); `SocketAddressResolver` (domyślnie `Async`, opcjonalnie `Sync`) i `setAddressResolutionTimeout` pozwalają podmienić DNS i przypiąć adres ([SocketAddressResolver](https://javadoc.jetty.org/jetty-12/org/eclipse/jetty/util/SocketAddressResolver.html), [threads](https://jetty.org/docs/jetty/12.1/programming-guide/client/threads.html)). HTTP/2 negocjowane przez ALPN (`setUseALPN(true)` na kliencie/transporcie HTTP/2) ([Jetty HTTP client 12.1](https://jetty.org/docs/jetty/12.1/programming-guide/client/http.html)).
- **Koszt:** brak reaktywnego stosu po stronie serwera, ale listenery nie mogą blokować; klient Jetty to kolejna biblioteka obok Springa, a wersja Jetty jest związana z Boot.

#### 2.3 Wariant mieszany: Spring MVC na wątkach wirtualnych + async klient w executorze

- **Serwer:** MVC na Tomcat/Jetty + `spring.threads.virtual.enabled=true`; żądania API obsługiwane jak w opcji 1.
- **Checki:** scheduler/executor uruchamia check na wątku wirtualnym, ale sam HTTP idzie przez Reactor Netty `HttpClient` albo Jetty `HttpClient` asynchronicznie; wynik wraca do executora i jest zapisywany przez JDBC. Callbacki klienta nie mogą blokować pętli zdarzeń/threadów I/O.
- **Zysk:** pojedyncze fazy (DNS/TCP/TLS/TTFB) z callbacków, bez przepisywania API na `Mono`/`Flux` i bez R2DBC; JDBC zostaje blokujące.
- **Koszt:** dwa modele w jednym procesie (wątki wirtualne + pętla zdarzeń), dwie pulle i dwie ścieżki błędów; RAM i CPU dla 5 000 checków **niepotwierdzone** — wymaga load testu. To wariant najbliższy „wrapperowi” z feedbacku, ale nie jest darmowy.

#### 2.4 Porównanie klientów checków: Reactor Netty, Jetty, OkHttp 5

Wszystkie trzy dają fazy per żądanie, ale różnią się modelem i miejscem pomiaru. Reactor Netty: callbacki lifecycle + `ConnectionObserver`, metryki Micrometer jako agregaty. Jetty: `Request.Listener`/`Response.Listener` + `Connection.Listener`, `onBegin` jako pierwsze bajty odpowiedzi, wprost reklamowane „accurately time every step”. OkHttp 5: `EventListener` z parami `dnsStart/dnsEnd`, `connectStart/connectEnd`, `secureConnectStart/secureConnectEnd`, `responseHeadersStart/responseHeadersEnd` i `EventListener.Factory` na `Call` — to także pełna oś czasu jednego wywołania, ale klient jest blokujący (wątek na check albo dispatcher), więc naturalnie łączy się z wątkami wirtualnymi ([OkHttp EventListener](https://square.github.io/okhttp/5.x/okhttp/okhttp3/-event-listener/index.html), [OkHttp events](https://square.github.io/okhttp/features/events/)). OkHttp nie daje gotowych metryk per faza — trzeba je zebrać samemu z listenera; Jetty i Reactor Netty mają listenery oraz (Reactor Netty) wbudowane metryki. Resolver: Reactor Netty `AddressResolverGroup`/`DnsNameResolver`, Jetty `SocketAddressResolver`; resolver i przypięcie IP w OkHttp są poza zakresem tej iteracji — **niepotwierdzone**.

### 3. Java + Quarkus albo Micronaut (JVM i native)

**Quarkus.** 3.40 LTS wydany 2026-09-30, wsparcie do 2027-09-30; 3.33 LTS od 2026-03-25 do 2027-03-25; minor co 4–6 tygodni, LTS co 6 miesięcy z 12-miesięcznym wsparciem ([endoflife.date/quarkus-framework](https://endoflife.date/quarkus-framework), [polityka LTS](https://quarkus.io/blog/lts-releases/)). Pomiary Java 25: Quarkus 3.28.1 JVM start 1,154 s, RSS 271,2 MB; native 0,049 s i 70,5 MB ([Gillius](https://gillius.org/blog/2025/10/java-25-framework-startup.html)). Inny pomiar: 3.40.1 JVM 2,185 s/171,6 MB, native 0,035 s/33,6 MB ([ankurm](https://ankurm.com/spring-boot-4-1-vs-quarkus-vs-micronaut-benchmarked/)). Serwer Postgres/JSON: JVM gotowy po 710 ms, stabilny RSS 129,1 MiB; native 81 ms i 27,3 MiB ([retheviper, 2026-04-29](https://blog.retheviper.com/en/posts/serverless-jvm-graalvm-benchmark)). Ekosystem: OIDC, Flyway/Liquibase, OpenAPI, Testcontainers — mniejszy niż Spring; native wymaga GraalVM/Mandrel i konfiguracji refleksji. Quarkus publikuje też własny benchmark (2,7× throughput, 269 vs 583 MB) — to materiał dostawcy, nie niezależny ([Quarkus](https://quarkus.io/blog/new-benchmarks/)).

**Micronaut.** 5.0 wydany 2026-05-13, 5.2.25 — 2026-11-17; 4.x wsparcie skończyło się 2026-05-13; brak oznaczonego LTS w endoflife ([endoflife.date/micronaut](https://endoflife.date/micronaut)). Pomiary: 4.5.4 JVM 0,656 s/253,2 MB, native 0,050 s/83,8 MB ([Gillius](https://gillius.org/blog/2025/10/java-25-framework-startup.html)); 5.2.2 JVM 2,507 s/180,0 MB, native 0,041 s/46,2 MB ([ankurm](https://ankurm.com/spring-boot-4-1-vs-quarkus-vs-micronaut-benchmarked/)). Ekosystem: Micronaut Security/OIDC, Data JDBC, Flyway, OpenAPI; mniejsza społeczność, szybszy cykl majorów i native-first.

**Wspólny kod z agentem:** brak. **Koszt:** dwa ekosystemy (Spring/Quarkus/Micronaut) do poznania, native build i debugowanie refleksji; dla jednej osoby to dodatkowy koszt bez wyraźnego zysku przy 83 checkach/s.

### 4. Kotlin + Spring Boot (korutyny)

- **Wersje:** Kotlin 2.4.0 (2026-06-03), 2.4.21 (2026-10-08); wydanie językowe co 6 miesięcy; stdlib JVM ma 18-miesięczne wsparcie bezpieczeństwa, dla 2.4 do 2027-12-03 ([Kotlin releases](https://kotlinlang.org/docs/releases.html)).
- **Model:** korutyny z `kotlinx-coroutines-reactor` w WebFlux albo `Dispatchers.IO` wokół blokującego JDBC. Korutyny nie usuwają limitu połączeń DB; przy JDBC wracamy do puli wątków. Spring Boot 4 wspiera Kotlin 2.x, ale korutyny nie dają przewagi nad wątkami wirtualnymi przy tym profilu I/O.
- **Pamięć i start:** dla Spring + Kotlin podobnie jak opcja 1; dla Ktor JVM pomiar 641 ms i 134,1 MiB, native 243 ms i 28,9 MiB ([retheviper](https://blog.retheviper.com/en/posts/serverless-jvm-graalvm-benchmark)) — to inny framework, nie Spring.
- **Ekosystem:** Spring + dodatkowe zależności Kotlin/kotlinx; mniejsza pula osób znających Kotlin + Spring Boot + korutyny.
- **Wspólny kod z agentem:** brak.
- **Wniosek:** sensowne tylko wtedy, gdy właściciel chce Kotlina z powodów osobistych/zespołowych; technicznie to Java z dodatkową warstwą złożoności.

### 5. Go dla całego serwera (stdlib `net/http` + mały router)

- **Wersje i wsparcie:** Go 1.27.4 (2026-11-10), 1.26.11 (2026-11-10); 1.25 wygasł 2026-08-19. Wspierane są dwa ostatnie wydania major; Go 1 ma promise kompatybilności ([Go release history](https://go.dev/doc/devel/release), [endoflife.date/go](https://endoflife.date/go)).
- **Model współbieżności:** gorutyna na połączenie/żądanie; stos startuje z kilku KB i rośnie; setki tysięcy gorutyn w jednym procesie są praktyczne ([Go FAQ](https://go.dev/doc/faq#goroutines)). `context` do anulowania, semafory/`errgroup` do limitów, `net/http` bez frameworka. Brak event loop i brak pinningu.
- **Pamięć, start i obraz:** NodeBook (2026-10, dynamiczny JSON, laptop, krótkie okna, mediana 3 przebiegów): Go `net/http` 16,7 MiB peak RSS i 73,5 tys. req/s; Node 97,9 MiB i 56,3 tys.; Rust/Axum 5,9 MiB i 89,1 tys. ([The Node Book](https://www.thenodebook.com/blog/nodejs-deserves-some-respect)). NpgsqlRest (2026-07, PostgreSQL, 6 endpointów, równe rdzenie): Go 59 MB peak/25 MB średnio, 90% CPU ([analiza](https://npgsqlrest.github.io/blog/benchmarks-2026-07/analysis.html)). Karnwong (2024, Docker/K8s, 5 000 VU/120 s): obraz Go 13,4 MB, peak 52,5 MB ([karnwong](https://karnwong.me/posts/2024/09/hello-world-api-performance-benchmark-go-node-python-rust/)). Start: pojedyncza binarka, brak VM — liczbowo **niepotwierdzone**, ale rzędu ms.
- **Ekosystem:** stdlib `net/http`, `crypto/tls`, `crypto/x509`, `encoding/json`, `log/slog`, `database/sql`; do tego `pgx` (PostgreSQL), `modernc.org/sqlite` (embedded), `go-oidc` + `oauth2` (OIDC), `goose`/`golang-migrate` (migracje), `oapi-codegen` (OpenAPI), `testcontainers-go`, `x/crypto` (Argon2id). Biblioteki są dojrzałe, ale trzeba więcej glue niż w Springu.
- **Baza embedded:** Go nie ma dojrzałego sterownika HSQLDB (HSQLDB jest Javowe). Naturalny zamiennik to SQLite (`modernc.org/sqlite`, czysty Go) albo PostgreSQL. To zmienia decyzję ADR 0001 o HSQLDB.
- **Wspólny kod z agentem:** tak — ten sam język, ten sam klient HTTP, fazy DNS/TLS, walidacja certyfikatów, asercje i kategorie błędów; parity testy stają się zwykłymi testami jednostkowymi. Jeden toolchain i jeden release pipeline zamiast dwóch.
- **Koszt utrzymania:** mały zestaw zależności, brak runtime, kompatybilność Go 1; więcej własnego kodu na OIDC/sesje/role/API. 5 000 checków to dla Go rutyna — Gatus i Statping-ng pokazują ten model.

### 6. TypeScript/Node (Uptime Kuma/OpenStatus) oraz krótko .NET i Rust

**Node.** Node 26 LTS od 2026-05-05, aktywne wsparcie do 2027-10-27, bezpieczeństwo do 2029-04-30; Node 24 LTS do 2028-04-30; major co 6 miesięcy, LTS ~30 miesięcy ([Node releases](https://nodejs.org/en/about/previous-releases), [endoflife.date/nodejs](https://endoflife.date/nodejs)). Model: jedna pętla zdarzeń + pula libuv; I/O działa dobrze, ale CPU-bound (crypto, JSON, regex) blokuje wszystkich — trzeba worker threads/cluster ([Node docs](https://nodejs.org/en/learn/asynchronous-work/dont-block-the-event-loop)). Pamięć: NodeBook 97,9 MiB peak i 56,3 tys. req/s (dynamiczny JSON, laptop); NpgsqlRest Express z 8 procesami: 1 094 MB peak/577 MB średnio (równe rdzenie, PostgreSQL); karnwong: obraz Node 130 MB, peak 203 MB przy 5 000 VU (2024). Ekosystem npm jest najbogatszy, ale zależności i churn są największe; OIDC `openid-client`, migracje Drizzle/Knex, OpenAPI przez zod, testy Testcontainers. Wspólnego kodu z agentem Go brak, ale jest wspólny język z frontendem (typy przez OpenAPI). Uptime Kuma 3.0.0-beta.1 wymaga Node ≥ 26.2 i działa w jednym procesie z Express 4 i `croner`; OpenStatus ma osobne `apps/checker`, `private-location` i `workflows`, a serwer na Hono/Node; Kener 4.2.2 to SvelteKit + Express + BullMQ/Redis + `croner` ([Kuma package.json](https://github.com/louislam/uptime-kuma/blob/master/package.json), [OpenStatus](https://github.com/openstatusHQ/openstatus/tree/main/apps/checker), [Kener package.json](https://github.com/rajnandan1/kener/blob/main/package.json)).

**.NET (krótko).** .NET 10 LTS od 2025-11-11 do 2028-11-14; .NET 11 STS od 2026-11-10 do 2028-11; łatki miesięczne; ASP.NET Core/Kestrel, async/await, OpenIddict/EF Core/Testcontainers ([Microsoft](https://learn.microsoft.com/en-us/dotnet/core/releases-and-support)). Dojrzały i wydajny, ale drugi język obok agenta Go; w open-source'owych monitorach uptime rzadki. Pamięć/start: **niepotwierdzone** dla tego profilu.

**Rust (krótko).** Wydania co 6 tygodni, brak LTS; edycje co ~3 lata, Rust 2024 od 1.85.0 (2025-02-20) ([Rust Forge](https://forge.rust-lang.org/), [edition guide](https://doc.rust-lang.org/edition-guide/rust-2024/index.html)). Najniższa pamięć (Axum 5,9 MiB peak w NodeBook), ale async/Tokio, długie kompilacje i mniejsza pula kontrybutorów; przy jednej osobie koszt rozwoju przewyższa zysk.

### Jak robią inni

| Projekt | Język | Framework / runtime | Model wykonywania checków | Repo |
|---|---|---|---|---|
| Uptime Kuma | JavaScript/TypeScript | Node.js 26 + Express 4 + Vue 3 | **event loop Node**, jeden proces; harmonogram `croner` w procesie; SQLite | [louislam/uptime-kuma](https://github.com/louislam/uptime-kuma) |
| OpenStatus | TypeScript | Hono na Node + Next.js | **event loop Node** w `apps/checker`, `private-location`, `workflows`; checki w osobnych usługach/regionach; Turso/Tinybird | [openstatusHQ/openstatus](https://github.com/openstatusHQ/openstatus) |
| Gatus | Go | Fiber v2, jedna binarka | gorutyny i scheduler w procesie (nie event loop); SQLite/PostgreSQL; konfiguracja YAML | [TwiN/gatus](https://github.com/TwiN/gatus) |
| Healthchecks | Python | Django 6.1, WSGI | pingi HTTP/email do bazy; osobna pętla `sendalerts` (systemd/supervisor) wysyła alerty; PostgreSQL/MySQL/MariaDB | [healthchecks/healthchecks](https://github.com/healthchecks/healthchecks) |
| Checkmate | TypeScript | Node 24 + Express 5 + React/MUI | **event loop Node** w API + osobny proces `QUEUE_MODE=worker`; MongoDB | [bluewave-labs/Checkmate](https://github.com/bluewave-labs/Checkmate) |
| Statping-ng | Go + Vue | Go backend, jedna binarka | gorutyny w procesie (nie event loop); SQLite/MySQL/PostgreSQL; projekt mało aktywny (ostatni push 2025-06) | [statping-ng/statping-ng](https://github.com/statping-ng/statping-ng) |
| Upptime | Markdown + Node | GitHub Actions | scheduled workflow wykonuje checki i commituje wyniki; statyczna status page z gita (nie event loop w serwerze) | [upptime/upptime](https://github.com/upptime/upptime) |
| Kener | TypeScript | SvelteKit + Express, Node 24 | **event loop Node** + kolejka BullMQ/Redis; harmonogram `croner`; SQLite/MySQL/PostgreSQL | [rajnandan1/kener](https://github.com/rajnandan1/kener) |

Modele event-loop/async: Kuma, OpenStatus, Checkmate i Kener (Node). Ich problemy są opisane wprost: Kuma trzyma wszystko w jednym procesie i — według issue — rośnie w RAM oraz utrzymuje zamknięte sockety TCP/TLS przy monitorach TLS (regresja naprawiona w 2.5.5), a użytkownicy raportują wysokie zużycie pamięci przy kilkudziesięciu monitorach ([Kuma #7537](https://github.com/louislam/uptime-kuma/issues/7537), [Kuma #5654](https://github.com/louislam/uptime-kuma/issues/5654)). To skutek modelu „wszystko w jednej pętli” i współdzielonego procesu; OpenStatus i Checkmate rozdzielają checker/worker, ale pozostają w Node. Gatus i Statping-ng używają gorutyn, a Healthchecks WSGI z osobną pętlą alertów — czyli nie event-loop.

Wnioski z cudzych stacków: (a) Go dominuje tam, gdzie liczy się jedna binarka i self-host (Gatus, Statping-ng) i jest naturalnym językiem agenta; (b) Node dominuje w „batteries included” panelach i status page'ach (Kuma, OpenStatus, Checkmate, Kener), ale każdy z nich prędzej czy później oddziela worker od API; (c) Python/Django pojawia się w niszy heartbeatów (Healthchecks), a nie w checkach HTTP na dużą skalę; (d) w tej próbce nie ma ani jednego monitora uptime na Java/Spring — to nie wyrok, ale sygnał, że Java nie jest naturalnym wyborem dla tego typu produktu; (e) nie ma też monitora używającego Spring Boot jako wrappera z async klientem checków — ten wariant nie ma precedensu w niszy.

## Porównanie

### Wersje, wsparcie i breaking changes

| Opcja | Wersja na 2026-11-20 | Wsparcie | Cykl / breaking changes | Ryzyko 10 lat |
|---|---|---|---|---|
| Java 25 + Boot 4 MVC | Java 25.0.5; Boot 4.2.0, 4.1.3, 4.0.10 | Java 25 do 2030-09-30; Boot 4.1 OSS do 2027-07-31, 4.0 do 2026-12-31 | Java LTS co 2 lata, feature co 6 mies.; Boot minor co 6 mies., OSS ~13 mies.; Boot 4 to duży major (Framework 7, Jackson 3) | średnie: częste upgrades, ale przewidywalne |
| Java + WebFlux (wrapper) | Boot 4.2.0 + Reactor Netty 1.4.0 | jak wyżej | dodatkowo Reactor/Netty | średnie/wyższe: więcej kombinacji |
| Spring Boot + Jetty 12.1 (wrapper) | Boot 4.2.0 + Jetty 12.1.15 | jak wyżej | dodatkowo Jetty; wersja związana z Boot | średnie: zgodność Jetty z Boot |
| Spring MVC VT + async klient (mieszany) | Boot 4.2.0 + Reactor Netty 1.4.0 albo Jetty 12.1.15 | jak wyżej | dodatkowo jeden klient async | średnie: dwa modele wykonania |
| Quarkus | 3.40 LTS (2026-09-30) | do 2027-09-30; LTS co 6 mies. | minor co 4–6 tyg.; LTS 12 mies.; 2→3 było dużym przejściem | średnie: mniejsza społeczność |
| Micronaut | 5.2.25 (2026-11-17) | 4.x skończone 2026-05-13; brak LTS | major co ~2 lata, minor często | wyższe: szybki churn, mniejsza pula |
| Kotlin + Spring | Kotlin 2.4.21 (2026-10-08) | stdlib 2.4 do 2027-12-03; Spring jak wyżej | język co 6 mies.; K2/kompatybilność | średnie: dodatkowa warstwa |
| Go | 1.27.4 (2026-11-10) | dwa ostatnie major (1.26, 1.27) | ~6 mies.; Go 1 promise, brak breaking changes | niskie: stabilny język, mało zależności |
| Node | 26.13.1 LTS | aktywne do 2027-10-27, security do 2029-04-30 | major co 6 mies.; LTS 30 mies.; breaking w majorach | średnie/wyższe: churn npm i runtime |
| .NET | 10 LTS, 11 STS | 10 do 2028-11-14; 11 do 2028-11 | major co rok; LTS co 2 lata | średnie: drugi język |
| Rust | stable co 6 tyg.; edition 2024 | brak LTS | edycje co ~3 lata, kompatybilne | wyższe: koszt rozwoju/kompilacji |

### Pamięć, start i obraz (tylko pomiary ze źródłem)

| Stack | Start | RSS | Obraz | Warunki pomiaru | Źródło |
|---|---|---|---|---|---|
| Spring Boot 3.5.6 JVM | 1,909 s | 388,9 MB max | JAR + JRE, **niepotwierdzone** | Java 25, JDBC/Flyway/PostgreSQL, 1 tabela, JSON+HTML, lokalna maszyna | [Gillius](https://gillius.org/blog/2025/10/java-25-framework-startup.html) |
| Spring Boot 3.5.6 native | 0,104 s | 149,4 MB max | native, **niepotwierdzone** | jak wyżej | [Gillius](https://gillius.org/blog/2025/10/java-25-framework-startup.html) |
| Spring Boot 4.1.1 JVM | 6,318 s | 229,2 MB | **niepotwierdzone** | jeden REST „widget”, JVM, maszyna autora | [ankurm](https://ankurm.com/spring-boot-4-1-vs-quarkus-vs-micronaut-benchmarked/) |
| Spring Boot 4.1.1 native | 0,132 s | 79,5 MB | **niepotwierdzone** | jak wyżej | [ankurm](https://ankurm.com/spring-boot-4-1-vs-quarkus-vs-micronaut-benchmarked/) |
| Spring Boot 4.1.0 + virtual threads | — | 404 MB średnio, 756 MB peak | **niepotwierdzone** | PostgreSQL, 6 endpointów, równe rdzenie, 100 VU perf-test, CPU 98%/400% | [NpgsqlRest](https://npgsqlrest.github.io/blog/benchmarks-2026-07/spring-boot.html) |
| Quarkus 3.28.1 JVM | 1,154 s | 271,2 MB max | **niepotwierdzone** | Java 25, JDBC/Flyway/PostgreSQL, 1 tabela | [Gillius](https://gillius.org/blog/2025/10/java-25-framework-startup.html) |
| Quarkus 3.28.1 native | 0,049 s | 70,5 MB max | **niepotwierdzone** | jak wyżej | [Gillius](https://gillius.org/blog/2025/10/java-25-framework-startup.html) |
| Quarkus 3.40.1 JVM | 2,185 s | 171,6 MB | **niepotwierdzone** | jeden REST „widget” | [ankurm](https://ankurm.com/spring-boot-4-1-vs-quarkus-vs-micronaut-benchmarked/) |
| Quarkus 3.40.1 native | 0,035 s | 33,6 MB | **niepotwierdzone** | jak wyżej | [ankurm](https://ankurm.com/spring-boot-4-1-vs-quarkus-vs-micronaut-benchmarked/) |
| Quarkus JVM / native (Postgres) | 0,710 s / 0,081 s | 129,1 / 27,3 MiB stabilnie | **niepotwierdzone** | Java 25, PostgreSQL, JSON, mediana, środowisko serverless-like | [retheviper](https://blog.retheviper.com/en/posts/serverless-jvm-graalvm-benchmark) |
| Micronaut 4.5.4 JVM / native | 0,656 s / 0,050 s | 253,2 / 83,8 MB max | **niepotwierdzone** | Java 25, JDBC/Flyway/PostgreSQL | [Gillius](https://gillius.org/blog/2025/10/java-25-framework-startup.html) |
| Micronaut 5.2.2 JVM / native | 2,507 s / 0,041 s | 180,0 / 46,2 MB | **niepotwierdzone** | jeden REST „widget” | [ankurm](https://ankurm.com/spring-boot-4-1-vs-quarkus-vs-micronaut-benchmarked/) |
| Ktor JVM / native (Kotlin) | 0,641 s / 0,243 s | 134,1 / 28,9 MiB stabilnie | **niepotwierdzone** | Java 25, PostgreSQL, JSON, mediana | [retheviper](https://blog.retheviper.com/en/posts/serverless-jvm-graalvm-benchmark) |
| Go `net/http` | brak liczby, **niepotwierdzone** | 16,7 MiB peak | **niepotwierdzone** | dynamiczny JSON, laptop, krótkie okna, mediana 3 przebiegów | [The Node Book](https://www.thenodebook.com/blog/nodejs-deserves-some-respect) |
| Go (PostgreSQL) | — | 25 MB średnio, 59 MB peak | **niepotwierdzone** | 6 endpointów, równe rdzenie, PostgreSQL, 90% CPU | [NpgsqlRest](https://npgsqlrest.github.io/blog/benchmarks-2026-07/analysis.html) |
| Go (Docker/K8s) | — | 52,48 MB peak | 13,4 MB | 5 000 VU/120 s, Docker/K8s, 2024 | [karnwong](https://karnwong.me/posts/2024/09/hello-world-api-performance-benchmark-go-node-python-rust/) |
| Node `node:http` | brak liczby, **niepotwierdzone** | 97,9 MiB peak | **niepotwierdzone** | dynamiczny JSON, laptop, krótkie okna | [The Node Book](https://www.thenodebook.com/blog/nodejs-deserves-some-respect) |
| Node/Express (8 procesów) | — | 577 MB średnio, 1 094 MB peak | **niepotwierdzone** | 6 endpointów, równe rdzenie, PostgreSQL | [NpgsqlRest](https://npgsqlrest.github.io/blog/benchmarks-2026-07/analysis.html) |
| Node (Docker/K8s) | — | 203,17 MB peak | 130 MB | 5 000 VU/120 s, 2024 | [karnwong](https://karnwong.me/posts/2024/09/hello-world-api-performance-benchmark-go-node-python-rust/) |
| Rust/Axum | — | 5,9 MiB peak | 8,63 MB | dynamiczny JSON, laptop; obraz 2024 | [The Node Book](https://www.thenodebook.com/blog/nodejs-deserves-some-respect), [karnwong](https://karnwong.me/posts/2024/09/hello-world-api-performance-benchmark-go-node-python-rust/) |

Uwaga: TechEmpower to benchmark syntetyczny i od 2026-03 zarchiwizowany — mierzy „sufit” frameworka, nie aplikację, więc nie używam go jako dowodu dla wakey ([repo](https://github.com/TechEmpower/FrameworkBenchmarks)). Pomiary wyżej pochodzą z różnych maszyn i konfiguracji — służą do rzędu wielkości, nie do wyłonienia „zwycięzcy” co do MB.

### Model wykonania checków: VT + blokujący klient vs pełny async vs mieszany

Dla żadnego z trzech wariantów nie ma publicznego pomiaru dla profilu wakey (5 000 checków I/O-bound na 1–2 vCPU); liczby RAM/CPU są **niepotwierdzone**. Podstawa jakościowa: liczba wątków/event loop, sposób pomiaru faz i ryzyko blokady ([JEP 444](https://openjdk.org/jeps/444), [Reactor Netty event loop](https://projectreactor.io/docs/netty/release/reference/http-client.html), [Jetty listenery](https://jetty.org/docs/jetty/12.1/programming-guide/client/http.html)).

| Wariant | RAM przy ~5 000 checków | CPU | Złożoność kodu i debugowania | Ryzyko błędów | Kontrybutorzy / wsparcie |
|---|---|---|---|---|---|
| VT + blokujący klient (JDK HttpClient/OkHttp) | baseline JVM + stos wątków; **niepotwierdzone** | wątek na check w czasie I/O; przy ~83/s nie problem; koszt context switch | najniższa: zwykły stack trace, profiler wątków, testy jak MVC | niskie: pinning (JEP 491), limity puli Hikari | wysoka: Java 25 LTS + Boot 4.1/4.2 |
| Pełny async (WebFlux + Reactor Netty, albo Jetty client) | mniej wątków, ale Netty/Reactor i bufory; **niepotwierdzone** | najlepsze przy wielu połączeniach; jedna blokada dławi pętlę | wysoka: `Mono`/`Flux` albo listenery, BlockHound, backpressure | wysokie: blokowanie event loop, wycieki buforów, błędy timeoutów | wysoka: Reactor/Netty/Jetty wspierane przez Boot |
| Mieszany (MVC VT + async klient w executorze) | pośredni; **niepotwierdzone** | dobry, jeśli callbacki nie blokują; dwa modele | średnia: dwie ścieżki i granica executor/event loop | średnie: podwójne pulle, różne modele błędów | wysoka: Boot + Reactor Netty/Jetty |

### Ekosystem potrzebny wakey

| Potrzeba | Spring Boot | Quarkus/Micronaut | Go | Node | Kotlin/Spring |
|---|---|---|---|---|---|
| OIDC login (Authorization Code + PKCE) | Spring Security OAuth2 Client, dojrzałe | Quarkus OIDC / Micronaut Security | `go-oidc` + `oauth2` + własna sesja | `openid-client` | jak Spring |
| OIDC resource server/JWT | Spring Security | Quarkus OIDC / Micronaut | `go-oidc` + middleware | `jose`/`openid-client` | jak Spring |
| Migracje | Liquibase/Flyway | Flyway/Liquibase | `goose`/`golang-migrate` | Drizzle/Knex/Prisma | Liquibase/Flyway |
| OpenAPI | springdoc | SmallRye OpenAPI | `oapi-codegen` | zod/OpenAPI | springdoc |
| Testy na PostgreSQL/HSQLDB | Testcontainers + HSQLDB | Testcontainers + HSQLDB | `testcontainers-go` + SQLite (HSQLDB brak) | Testcontainers + SQLite | jak Spring |
| Klient HTTP z fazami | JDK HttpClient/`httptrace`-like | Vert.x/klient | `net/http/httptrace` | undici/fetch | jak Spring |
| Szyfrowanie sekretów | JCE | JCE | `crypto/aes` + `x/crypto` | `node:crypto` | JCE |
| Role/audyt | Spring Security + własne | własne | własne | własne | jak Spring |

### KISS i koszt utrzymania

| Opcja | Toolchainy | Zależności runtime | Kto utrzyma | Główny koszt |
|---|---|---|---|---|
| Java 25 + Boot 4 MVC | Java + Maven (+ Go agent) | dziesiątki (Spring, Hikari, Liquibase, springdoc, sterowniki) | osoba znająca Javę | upgrades Boot co 6–12 mies., pamięć, zgodność bibliotek |
| Java + WebFlux (wrapper) | jak wyżej | jeszcze Reactor/Netty/R2DBC | jak wyżej | złożoność reaktywna, HSQLDB |
| Spring + Jetty (wrapper) | Java + Maven + Jetty (+ Go) | dziesiątki + Jetty | osoba znająca Springa | listenery, zgodność Jetty/Boot |
| Spring MVC VT + async klient | Java + Maven + Reactor Netty/Jetty (+ Go) | dziesiątki + jeden klient | osoba znająca Springa | dwa modele, dyscyplina executorów |
| Quarkus | Java + Maven + GraalVM/Mandrel (+ Go) | dziesiątki, mniejsze | osoba znająca Quarkusa | native build, mniejszy ekosystem |
| Micronaut | Java + build + GraalVM (+ Go) | dziesiątki | osoba znająca Micronauta | churn majorów, mniejsza pula |
| Kotlin + Spring | Kotlin + Gradle/Maven + JVM (+ Go) | dziesiątki + kotlinx | osoba znająca Kotlin/Spring | korutyny + JDBC, mniejsza pula |
| Go | Go (+ Go agent) — **jeden** | ~6 bezpośrednich + stdlib | osoba znająca Go | własny glue OIDC/API, SQLite zamiast HSQLDB |
| Node | Node + npm (+ Go agent) | setki (drzewo npm) | osoba znająca TS/Node | churn, event loop, supply chain |
| .NET | .NET + NuGet (+ Go agent) | dziesiątki | osoba znająca C# | drugi język, mniejsza nisza |
| Rust | Rust + Cargo (+ Go agent) | dziesiątki | osoba znająca Rusta | czas rozwoju/kompilacji, async |

**Wniosek z porównania:** 5 000 checków nie wymaga WebFlux ani native. Wymagania pamięciowe i „jedna binarka” faworyzują Go; wymagania OIDC/CRUD/ekosystemu faworyzują Spring. HSQLDB jest punktem zwrotnym: w Go trzeba ją zastąpić SQLite, w Javie zostaje. Dwa toolchainy (Java + Go agent) to realny koszt, którego Go nie ma.

## Własna implementacja

Niezależnie od stacku serwer musi mieć: executor/scheduler, silniki checków (HTTP/TCP/ICMP/certyfikat/heartbeat), leader lease z fencingiem, API + role + audyt + OIDC, kanały alertów, migracje i testy na dwóch bazach. Szacunek dla MVP serwera (bez frontendu i agenta): 15–25 tys. linii i 4–7 miesięcy jednej osoby; poniżej rozbicie.

| Obszar | Co samemu | Szacunek |
|---|---|---|
| Executor i scheduler | kolejka, semafory per lokalizacja, backpressure, batch zapisu, harmonogram cron/okres | 1,5–2,5 tys. LOC, 15–25 dni |
| Checki HTTP/TCP/ICMP/cert/heartbeat | fazy DNS/connect/TLS/TTFB, asercje, kategorie błędów, łańcuch certyfikatów, ICMP bez roota, heartbeat | 3–5 tys. LOC, 30–45 dni |
| Leader i fencing | lease w bazie, TTL, odnowienie, fencing token, idempotencja | 0,8–1,5 tys. LOC, 10–15 dni |
| API, role, audyt, OIDC | RBAC, sesje, OIDC, rate limit, walidacja, OpenAPI, audyt | 3–6 tys. LOC, 30–45 dni |
| Alerty i kanały | polityki, ack, snooze, zbiorczy alert, retry, log dostarczeń, martwy kanał | 2–4 tys. LOC, 20–30 dni |
| Baza i migracje | schemat, wspólny podzbiór SQL, migracje, backup/restore, szyfrowanie sekretów | 1–2 tys. LOC, 10–15 dni |
| Testy i CI | unit, kontrakt, Testcontainers/Postgres, plik SQLite/HSQLDB, load 5 000 checków, chaos | 2–3 tys. LOC, 20–30 dni |

Różnice per opcja:
- **Spring MVC:** najmniej glue dzięki Security OIDC, Liquibase, springdoc i Testcontainers; samemu zostaje executor, leader, semantyka checków i wspólny podzbiór SQL. Trudne: pinning w JDBC/DNS (Java 25 łagodzi), dobór puli Hikari, testy HSQLDB vs PostgreSQL, upgrade Boot.
- **WebFlux:** dodatkowo własne granice blokowania, R2DBC dla części zapytań, testy reaktywne; HSQLDB praktycznie wypada. Trudne: debugowanie i backpressure.
- **Spring + Jetty 12:** własne listenery faz, mapowanie błędów i osobny executor; JDBC zostaje. Trudne: utrzymanie zgodności Jetty z Boot, dyscyplina „nie blokuj listenera”, testy ALPN/HTTP2 i resolvera.
- **Spring mieszany:** najwięcej dyscypliny: jedna ścieżka blokująca (API/JDBC) i jedna async (checki), wspólny model timeoutów i błędów; testy muszą pilnować, że callback nie blokuje. Trudne: dwie pulle, anulowanie checku i spójny zapis wyniku.
- **Quarkus/Micronaut:** własny executor/leader i semantyka checków; część extensionów trzeba zastąpić własnym kodem. Trudne: native image (refleksja, zasoby), build i testy native, mniejsza baza przykładów.
- **Go:** najmniej kodu na checki dzięki wspólnemu silnikowi z agentem; najwięcej na OIDC/sesje/role/API i na warstwę SQL (pgx + SQLite). Trudne: bezpieczeństwo sesji, CSRF, PKCE, rotacja sekretów, różnice SQLite/PostgreSQL, single-writer SQLite, backup spójny.
- **Node:** własny silnik checków, izolacja CPU (worker threads/checker), backpressure, OIDC `openid-client`, migracje i testy; trudne: event loop, pamięć pod obciążeniem, churn zależności.
- **Kotlin:** jak Spring + korutyny; trudne: mieszanie blokującego JDBC z korutynami, mniejsza pula osób.

Wspólne trudne miejsca: pomiar faz HTTP, własny resolver z przypięciem adresu (DNS rebinding), łańcuch certyfikatów i własne CA, ICMP bez roota, strefy czasowe i cron, leader lease przy split-brain, rotacja klucza sekretów, limity i 429, backup/restore z odszyfrowaniem. Testy: wektory kontraktowe wspólne z agentem, Testcontainers PostgreSQL, plik SQLite/HSQLDB, Keycloak do OIDC, k6/wrk z 5 000 checków, chaos (baza down, clock skew, DNS failure), odtworzenie backupu z kluczem.

## Rekomendacja

**Rekomendacja główna: Go 1.27 + stdlib `net/http` + mały router (`ServeMux` 1.22+), PostgreSQL (`pgx`) w chmurze i SQLite (`modernc.org/sqlite`, czysty Go) w self-host; OIDC przez `go-oidc` + `oauth2`; migracje `goose`; OpenAPI `oapi-codegen`.** To świadoma zmiana względem ADR 0001 w dwóch punktach: serwer w Go i SQLite zamiast HSQLDB. Uzasadnienie: 5 000 checków to ~83/s, więc wydajność HTTP nie jest problemem; wygrywają koszt bazowy (25–60 MB vs 250–400 MB JVM), jedna binarka/obraz (13–20 MB vs setki MB z JRE), jeden język i toolchain z agentem, wspólny kod checków oraz mały zestaw zależności. Gatus i Statping-ng dowodzą, że ten model działa w tej niszy. Cena: własny glue OIDC/sesji/API i rezygnacja z HSQLDB.

**Wariant zapasowy: Java 25 LTS + Spring Boot 4.1.x/4.2.x MVC + wątki wirtualne.** Wybrać, jeśli HSQLDB jest twardym wymogiem, właściciel chce zostać w Javie albo ekosystem Spring Security/Liquibase/springdoc jest wart 250–400 MB baseline i drugiego toolchainu. Nie wybierać Boot 4.0 (OSS kończy się 2026-12-31) ani WebFlux (brak potrzeby). Boot 4.2.0 jest świeży (2026-11-19); do produkcji pin 4.2.x po bramce zgodności, w razie problemów 4.1.x.

**Jeśli zostajesz przy Springu, nie wybieraj pełnego WebFlux „bo async”.** Najmniejszy koszt daje MVC + VT + blokujący klient (opcja 1). Gdy fazy DNS/TCP/TLS/TTFB są wymagane per check, wybierz wariant mieszany 2.3 z Jetty `HttpClient` albo Reactor Netty `HttpClient` w executorze; pełny WebFlux (2.1) ma sens tylko wtedy, gdy API też ma być reaktywne. Jetty jako serwer (2.2) nie daje przewagi nad Tomcatem przy tym profilu, a Undertow nie jest opcją w Boot 4.

| Potrzeba | Rekomendacja |
|---|---|
| Serwer | Go 1.27 (główna) / Java 25 + Boot 4.1–4.2 MVC (zapas) |
| Współbieżność | gorutyny + semafory (Go) / wątki wirtualne (Java) |
| Klient checków (Java, gdy fazy wymagane) | Jetty `HttpClient` albo Reactor Netty `HttpClient` w executorze (wariant mieszany 2.3) |
| Baza self-host | SQLite plikowy (Go) / HSQLDB plikowy (Java) |
| Baza chmura | PostgreSQL + `pgx` / PostgreSQL + JDBC |
| Migracje | `goose` / Liquibase |
| OIDC | `go-oidc` + `oauth2` + własna sesja / Spring Security |
| OpenAPI | `oapi-codegen` / springdoc |
| Testy DB | `testcontainers-go` + SQLite / Testcontainers + HSQLDB |
| Agent | Go (bez zmian) |
| Frontend | React + Vite (bez zmian) |

Łączna lista zależności runtime dla wariantu Go: `github.com/jackc/pgx/v5` (PostgreSQL), `modernc.org/sqlite` (embedded), `github.com/coreos/go-oidc/v3` + `golang.org/x/oauth2` (OIDC), `github.com/pressly/goose/v3` (migracje), `golang.org/x/crypto` (Argon2id), `github.com/google/uuid`; reszta to stdlib (`net/http`, `crypto/tls`, `crypto/x509`, `encoding/json`, `log/slog`, `database/sql`, `context`, `time`). Narzędzia deweloperskie: `oapi-codegen`, `testcontainers-go`, `testify`, opcjonalnie `golangci-lint`. Dla wariantu Java: starters Spring Boot (web, security, oauth2-client, data-jdbc, actuator, validation), Liquibase, springdoc, sterownik PostgreSQL, HSQLDB, Testcontainers, HikariCP — kilkanaście bezpośrednich i dziesiątki przechodnich.

## Ryzyka

| Ryzyko | Mitygacja |
|---|---|
| Go: własne OIDC/sesje/CSRF są łatwe do zrobienia źle | użyć `go-oidc`, PKCE, krótkie sesje, rotacja, testy z Keycloakiem, przegląd bezpieczeństwa |
| Go: brak HSQLDB, SQLite ma jednego writera | WAL + `busy_timeout`, jeden proces, kolejka zapisu, backup spójny; test 5 000 checków na pliku |
| Go: mniej gotowych bibliotek CRUD/admin | `sqlc`/`oapi-codegen`, małe handlery, biblioteka tylko tam, gdzie własny kod byłby duży |
| Go: mniejsza znajomość wśród kontrybutorów Javy | dokumentacja, „good first issues”, Go jest popularne w monitoringu (Gatus, Statping-ng) |
| Go: pamięć przy 5 000 checków jest szacunkiem, nie pomiarem | spike/load test przed decyzją; mierzyć RSS, GC, opóźnienie zapisu |
| Java: Boot 4.0 wygasa 2026-12-31 | pin 4.1.x/4.2.x; bramka zgodności springdoc/Liquibase/Testcontainers/Data JDBC |
| Java: 250–400 MB baseline na VPS 2 GB | `-XX:MaxRAMPercentage`, mniejszy heap, native opcjonalnie; mierzyć z PostgreSQL |
| Java: pinning w JDBC/DNS na wątkach wirtualnych | Java 25 + JEP 491, testy pinningu, `ReentrantLock`, pula Hikari niezależna od liczby wątków |
| Java: dwa toolchainy i duplikacja semantyki checków | wspólne wektory kontraktowe i testy parity; albo wybrać Go |
| Spring wrapper: blokowanie event loop w callbacku async klienta | zakaz blokowania, BlockHound w testach, timeouty, osobny executor dla checków |
| Spring wrapper: dwa modele i dwie pulle (VT + event loop) | jedna abstrakcja klienta checków, wspólne metryki i testy kontraktowe; mierzyć lag pętli |
| Jetty/Reactor Netty: wersja biblioteki związana z Boot | pin Boot + bramka zgodności, nie mieszać wersji ręcznie; testy ALPN i resolvera |
| Brak pomiaru RAM/CPU dla 5 000 checków w wariantach wrappera | load test na 1–2 vCPU przed decyzją; mierzyć RSS, lag pętli i opóźnienie zapisu |
| Quarkus/Micronaut: native build i mniejszy ekosystem | spike native, fallback Spring; nie wybierać bez twardego wymogu native |
| Node: event loop blokuje przy crypto/JSON/regex | worker threads lub osobny `checker`, load test, limity wejścia |
| Node: churn zależności i supply chain | Renovate, lockfile, minimalizm, przegląd nowych paczek |
| Kotlin: korutyny + blokujące JDBC | nie łączyć bez potrzeby; zostać przy Javie |
| .NET/Rust: drugi język i mniejsza pula | odłożyć; nie teraz |
| Wspólne: 5 000 checków bez definicji szczytu | ustalić interwał, timeout, kworum i szczyt; load test na 1–2 vCPU |
| Wspólne: OIDC/backup/rotacja sekretów | macierz dostawców, test odtworzenia backupu z kluczem, procedura rotacji |

## Pytania do grill-me

1. Czy HSQLDB w pliku jest twardym wymogiem, czy SQLite może ją zastąpić? To rozstrzyga, czy Go jest w ogóle dopuszczalne.
2. Czy akceptujesz dwa toolchainy (Java + Go agent) i duplikację semantyki checków, czy wolisz jeden język (Go) i wspólny kod z agentem?
3. Jaki jest realny budżet pamięci na VPS 2 GB: czy 250–400 MB baseline JVM jest OK, czy chcesz <100 MB?
4. Czy OIDC ma obsługiwać tylko logowanie (Authorization Code + PKCE + sesja), czy także resource server/JWT dla API? To zmienia ilość własnego kodu w Go.
5. Czy GraalVM native image ma być w wydaniu (dłuższy build, refleksje, ograniczenia bibliotek), czy wystarczy JVM?
6. Co jest priorytetem: czas do MVP czy 10-letni koszt utrzymania? Inny wybór wyjdzie dla MVP, inny dla projektu na lata.
7. Czy zostajesz przy Maven/Java z powodu istniejącej wiedzy i ekosystemu, czy inwestujesz w Go?
8. Jak wygląda plan backupu i migracji dla SQLite/HSQLDB (single writer, locking, odtworzenie z kluczem)?
9. Czy OpenAPI ma być źródłem typów dla frontendu (codegen), czy wystarczy ręczny kontrakt?
10. Czy 5 000 checków to 5 000 aktywnych przy interwale 60 s, czy szczyt 5 000/s? Czy potrzebne są od razu wiele lokalizacji i kworum?
11. Czy wrapper Springa ma sens tylko po to, by zostać przy Spring Security/OIDC, czy naprawdę potrzebujesz async w samych checkach?
12. Czy fazy DNS/TCP/TLS/TTFB są wymagane per check, czy wystarczy łączny czas i status? To różnica między blokującym klientem a listenerami.
13. Czy akceptujesz dwa modele w jednym procesie (MVC VT + pętla Netty/Jetty) i dodatkową bibliotekę klienta zamiast jednego klienta JDK?
14. Czy serwer ma być Tomcat, Jetty czy Reactor Netty — i czy HTTP/2 przez ALPN jest wymagane dla checków?
15. Czy przed decyzją mierzymy RAM/CPU i lag pętli na VPS 1–2 vCPU, czy wystarczy szacunek i źródła?

## Źródła

- https://endoflife.date/oracle-jdk
- https://endoflife.date/spring-boot
- https://endoflife.date/quarkus-framework
- https://endoflife.date/micronaut
- https://endoflife.date/go
- https://endoflife.date/nodejs
- https://endoflife.date/dotnet
- https://openjdk.org/jeps/444
- https://openjdk.org/jeps/491
- https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes
- https://github.com/spring-projects/spring-boot/releases/tag/v4.2.0
- https://quarkus.io/blog/lts-releases/
- https://go.dev/doc/devel/release
- https://go.dev/doc/faq#goroutines
- https://nodejs.org/en/about/previous-releases
- https://nodejs.org/en/learn/asynchronous-work/dont-block-the-event-loop
- https://kotlinlang.org/docs/releases.html
- https://learn.microsoft.com/en-us/dotnet/core/releases-and-support
- https://forge.rust-lang.org/
- https://doc.rust-lang.org/edition-guide/rust-2024/index.html
- https://github.com/louislam/uptime-kuma
- https://github.com/openstatusHQ/openstatus
- https://github.com/TwiN/gatus
- https://github.com/healthchecks/healthchecks
- https://github.com/bluewave-labs/Checkmate
- https://github.com/statping-ng/statping-ng
- https://github.com/upptime/upptime
- https://github.com/rajnandan1/kener
- https://npgsqlrest.github.io/blog/benchmarks-2026-07/spring-boot.html
- https://npgsqlrest.github.io/blog/benchmarks-2026-07/analysis.html
- https://www.thenodebook.com/blog/nodejs-deserves-some-respect
- https://karnwong.me/posts/2024/09/hello-world-api-performance-benchmark-go-node-python-rust/
- https://gillius.org/blog/2025/10/java-25-framework-startup.html
- https://ankurm.com/spring-boot-4-1-vs-quarkus-vs-micronaut-benchmarked/
- https://blog.retheviper.com/en/posts/serverless-jvm-graalvm-benchmark
- https://quarkus.io/blog/new-benchmarks/
- https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html
- https://quarkus.io/guides/security-openid-connect
- https://pkg.go.dev/github.com/coreos/go-oidc/v3/oidc
- https://github.com/pressly/goose
- https://github.com/golang-migrate/migrate
- https://testcontainers.com/
- https://springdoc.org/
- https://github.com/brettwooldridge/HikariCP
- https://modernc.org/sqlite
- https://github.com/jackc/pgx
- https://github.com/oapi-codegen/oapi-codegen
- https://github.com/openstatusHQ/openstatus/tree/main/apps/checker
- https://github.com/bluewave-labs/Checkmate/blob/develop/server/package.json
- https://github.com/louislam/uptime-kuma/blob/master/package.json
- https://github.com/rajnandan1/kener/blob/main/package.json
- https://github.com/TwiN/gatus/blob/master/go.mod
- https://github.com/healthchecks/healthchecks/blob/master/requirements.txt
- https://github.com/TechEmpower/FrameworkBenchmarks
- https://docs.spring.io/spring-boot/how-to/webserver.html
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- https://docs.spring.io/spring-boot/reference/web/reactive.html
- https://projectreactor.io/docs/netty/release/reference/http-client.html
- https://projectreactor.io/docs/netty/release/api/reactor/netty/ConnectionObserver.html
- https://projectreactor.io/docs/netty/release/api/reactor/netty/ConnectionObserver.State.html
- https://netty.io/4.2/api/io/netty/resolver/dns/DnsNameResolver.html
- https://netty.io/4.2/api/io/netty/handler/codec/dns/DnsRecordType.html
- https://jetty.org/docs/jetty/12.1/programming-guide/client/http.html
- https://jetty.org/docs/jetty/12.1/programming-guide/client/threads.html
- https://jetty.org/docs/jetty/12/programming-guide/client/http.html
- https://javadoc.jetty.org/jetty-12/org/eclipse/jetty/io/Connection.Listener.html
- https://javadoc.jetty.org/jetty-12/org/eclipse/jetty/util/SocketAddressResolver.html
- https://javadoc.jetty.org/jetty-12.1/org/eclipse/jetty/client/Response.Listener.html
- https://square.github.io/okhttp/5.x/okhttp/okhttp3/-event-listener/index.html
- https://square.github.io/okhttp/features/events/
- https://docs.micrometer.io/micrometer/reference/concepts/meters.html
- https://github.com/louislam/uptime-kuma/issues/7537
- https://github.com/louislam/uptime-kuma/issues/5654
