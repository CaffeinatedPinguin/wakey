# Research: ops — agent, budowanie, CI, dystrybucja i wydania wakey

## Zakres

Badam siedem decyzji operacyjnych wokół wakey: język agenta, jeden proces z rolami czy osobne procesy, narzędzie budowania serwera, monorepo czy wiele repozytoriów, minimalny CI na GitHub Actions, dystrybucję self-host oraz wersjonowanie i podpisywanie wydań. Nie badam protokołu agenta ani pomiarów sieciowych (to `docs/research/net-go.md`), wyboru bibliotek frontendu, dostawców UE i prawa. ADR 0001 traktuję jako jedną z opcji, bo właściciel otworzył punkt 1 backlogu. Uwaga: w tym worktree nie ma `docs/research/net-go.md`, więc nie mogłem się na nim oprzeć; szczegóły implementacji agenta Go zostawiam tamtemu dokumentowi i nie powtarzam ich.

## Opcje

### 1. Język agenta

**A. Go, statyczna binarka.** Gatus buduje `CGO_ENABLED=0` i uruchamia binarkę w obrazie `scratch`; gotowy obraz `stable` ma 26,4 MB ([Dockerfile](https://github.com/TwiN/gatus/blob/master/Dockerfile), [Docker Hub](https://hub.docker.com/r/twinproduction/gatus)). Statping-ng deklaruje binarkę ~17 MB i obraz ~20 MB ([README](https://github.com/statping-ng/statping-ng)). OpenStatus chwali się prywatną lokalizacją w obrazie 8,5 MB ([README](https://github.com/openstatusHQ/openstatus)). To najmniejszy artefakt, brak JRE, trywialny cross-compile `GOOS/GOARCH`, dobry stdlib do TLS/DNS/ICMP. Koszt: drugi toolchain i drugi pipeline.

**B. Ten sam język co serwer: Java.** Można użyć małego programu Java bez Springa, a nie kopiować serwer. `jlink` buduje runtime z tylko potrzebnych modułów; przykład Oracle dla `java.base` + locale to 23 MB po kompresji ([jlink, JDK 25](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jlink.html)). GraalVM Native Image daje binarkę z „ułamkiem zasobów JVM”, start w milisekundach i mniejszą powierzchnię ataku, ale wymaga metadanych dla refleksji i dłuższego buildu ([Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)). Realny artefakt to runtime jlink + JAR zależności albo binarka native; RAM JVM jest zwykle wyższy niż Go (szacunek, bez pomiaru). Zysk: jeden język, jedna wiedza, wspólne testy; koszt: więcej RAM na agenta i trudniejszy native build.

**C. Node.js lub inny runtime.** Uptime Kuma działa na Node.js >= 20.4 i obraz `latest` ma 156 MB ([README](https://github.com/louislam/uptime-kuma), [Docker Hub](https://hub.docker.com/r/louislam/uptime-kuma)). To wygodne, ale agent dostaje runtime, `node_modules` i większy obraz. Dla agenta w sieci prywatnej to nadmiar.

### 2. Jeden proces z rolami czy osobne procesy

**A. Jedna aplikacja, role konfiguracją.** Jeden artefakt, warunkowe beany, `app.roles=api,executor,alerts`. Self-host domyślnie wszystkie role; chmura może rozdzielić później. Tak działa Gatus (jeden binarny proces, [README](https://github.com/TwiN/gatus)) i w praktyce Uptime Kuma (jeden proces Node, [README](https://github.com/louislam/uptime-kuma)).

**B. Osobne procesy od początku.** Healthchecks wymaga trzech procesów: web, `sendalerts` i `sendreports` ([README](https://github.com/healthchecks/healthchecks)). Checkmate ma tryb `QUEUE_MODE=worker` i osobne workery ([README](https://github.com/bluewave-labs/checkmate)). OpenStatus publikuje osobne obrazy: server, dashboard, workflows, private-location, status-page, checker ([README](https://github.com/openstatusHQ/openstatus)). To poprawne dla chmury, ale w self-host zwiększa liczbę kontenerów, konfiguracji i miejsc awarii.

**C. Mikroserwisy i kolejka od dnia 1.** Największy koszt operacyjny i najmniejsza korzyść przy 5 000 checków i jednym opiekunie; wymaga brokera, siatki, obserwowalności i wersjonowania kontraktów.

### 3. Narzędzie budowania serwera JVM

**A. Maven.** Maven Wrapper przypina wersję i pozwala uruchomić build bez instalacji Mavena ([Maven Wrapper](https://maven.apache.org/wrapper/)). Model XML jest deklaratywny i stabilny; Spring Boot i większość wtyczek ma dobre wsparcie. Minus: wolniejszy przyrostowy build i mniej elastyczne skrypty.

**B. Gradle.** Gradle Wrapper jest rekomendowanym sposobem uruchamiania builda i również przypina wersję ([Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)). Zysk: szybsze buildy przyrostowe, configuration cache, Kotlin DSL; koszt: więcej ruchomych części i większa powierzchnia wiedzy. Dla jednej osoby to dodatkowy koszt, chyba że build Mavena przekroczy próg tolerancji.

**C. Odpowiednik poza JVM.** Dla Go: moduły Go (`go.mod`/`go.sum`) plus `go build`, a wydania przez GoReleaser ([Go Modules](https://go.dev/ref/mod), [GoReleaser nFPM](https://goreleaser.com/customization/nfpm/)). Dla Node: `npm ci` albo `pnpm` z lockfilem; nie jest to jednak rekomendacja zmiany serwera na Node.

### 4. Monorepo czy osobne repozytoria

**A. Monorepo.** `server/`, `agent/`, `web/`, `deploy/`, `docs/` w jednym repo; agent pozostaje osobnym modułem Go. Jedna zmiana kontraktu obejmuje serwer, agenta i testy parity w jednym PR; jeden tag wersji; prostsze Dependabot i przegląd. Tak trzymają serwer i UI Uptime Kuma, Gatus oraz Checkmate ([Kuma](https://github.com/louislam/uptime-kuma), [Gatus](https://github.com/TwiN/gatus), [Checkmate](https://github.com/bluewave-labs/checkmate)). Checkmate wydzielił jednak agenta Capture do osobnego repo ([Capture](https://github.com/bluewave-labs/capture)).

**B. Osobne repozytoria.** Niezależne wydania, uprawnienia i licencje; koszt: zsynchronizowana zmiana kontraktu wymaga dwóch PR, dwóch tagów i uzgodnienia wersji. Ma sens dopiero, gdy agent ma innych opiekunów lub inną licencję.

**C. Monorepo z niezależnymi wersjami artefaktów.** Kompromis: jedno repo, ale tagi `server-vX` i `agent-vX`. Więcej tagów i większe ryzyko rozjazdu niż jeden tag; do rozważenia tylko przy wyraźnym wymaganiu.

### 5. CI na GitHub Actions

**A. Minimalny pipeline na GitHub Actions.** Publiczne repozytorium ma darmowe standardowe runnery; dla prywatnego plan Free daje 12 USD miesięcznie wliczonego zużycia, a Linux 2-core kosztuje 0,006 USD/min ([billing](https://docs.github.com/en/billing/managing-billing-for-your-products/about-billing-for-github-actions)). Minimalny zestaw: (1) unit testy serwera, (2) integracja na HSQLDB w pliku i PostgreSQL w Testcontainers, (3) Liquibase na obu bazach, (4) testy i build agenta, (5) build frontendu, (6) obraz Docker i wydanie tylko na tag. PR-y uruchamiają 1–5; tag uruchamia 6.

**B. Więcej jobów i macierz.** Macierz OS/JDK/Go daje większe pokrycie, ale mnoży minuty i czas oczekiwania. Przy prywatnym repo 20 PR-ów × 10 min to ~200 min, czyli ~1,2 USD miesięcznie; publiczne repo to 0 USD (szacunek na podstawie cennika). Warto mierzyć, nie zgadywać.

**C. Self-hosted runner.** Zerowy koszt minut, ale utrzymanie maszyny, patchowanie i ryzyko, że runner ma dostęp do sekretów. Dla jednej osoby zwykle nie warto.

### 6. Dystrybucja self-host

**A. Jeden obraz Docker + docker compose.** Aplikacja serwuje UI i API; HSQLDB w pliku domyślnie, profil Postgres opcjonalnie. Compose definiuje usługi, sieci i wolumeny w jednym pliku ([Docker Compose](https://docs.docker.com/compose/)). Tak robią Uptime Kuma, Healthchecks i Checkmate: obraz + compose, baza jako druga usługa, gdy trzeba ([Kuma](https://github.com/louislam/uptime-kuma), [Healthchecks](https://github.com/healthchecks/healthchecks), [Checkmate](https://github.com/bluewave-labs/checkmate)).

**B. Wiele obrazów.** Osobno API, executor, alerty, frontend. Konieczne w chmurze przy niezależnym skalowaniu; w self-host MVP to więcej dokumentacji i błędów konfiguracji.

**C. Paczki deb/rpm i Helm.** GoReleaser + nfpm generuje `.deb`, `.rpm`, `.apk` i inne formaty ([GoReleaser nFPM](https://goreleaser.com/customization/nfpm/)). Helm pakuje zasoby Kubernetes w wersjonowane charty ([Helm Charts](https://helm.sh/docs/topics/charts/)). Oba są warte dodania po pierwszych użytkownikach; Helm ma sens, gdy pojawi się realny popyt na K8s. Datadog dostarcza worker jako obraz Docker, usługę Windows/MSI i chart Helm ([Datadog](https://docs.datadoghq.com/synthetics/private_locations/)) — to przykład pełnej macierzy, nie MVP.

**D. Tylko binarka dla agenta.** Agent to statyczna binarka + obraz Docker; nie pakujemy go do obrazu serwera. Statping-ng pokazuje, że binarka ~17 MB wystarcza do instalacji bez menedżera pakietów ([README](https://github.com/statping-ng/statping-ng)).

### 7. Wydania i aktualizacje

**A. Wersjonowanie SemVer + changelog.** SemVer 2.0.0 definiuje MAJOR.MINOR.PATCH i wymóg niezmienności wydania ([SemVer](https://semver.org/)); Keep a Changelog 1.1.0 daje sekcje Added/Changed/Deprecated/Removed/Fixed/Security ([Keep a Changelog](https://keepachangelog.com/en/1.1.0/)). Jeden tag `vX.Y.Z` dla serwera, UI i agenta; protokół agenta ma osobny numer zgodności.

**B. Podpisy.** Cosign/Sigstore podpisuje obrazy i blob-y; tryb keyless wiąże podpis z tożsamością OIDC i zapisuje zdarzenie w Rekor ([Sigstore](https://docs.sigstore.dev/cosign/signing/overview/), [blob](https://docs.sigstore.dev/cosign/signing/signing_with_blobs/)). Do tego `SHA256SUMS` i podpis binarek agenta. Klucz długotrwały jest prostszy offline, keyless jest wygodniejszy w GitHub Actions; wybór to pytanie do właściciela.

**C. Powiadomienie o nowej wersji.** Serwer odpytuje `GET /repos/{owner}/{repo}/releases/latest` i pokazuje wersję w stanie systemu ([GitHub Releases API](https://docs.github.com/en/rest/releases/releases)). To jedno połączenie wychodzące; musi mieć wyłącznik dla instalacji offline.

**D. Automatyczne aktualizacje.** Watchtower sam pobiera nowy obraz i restartuje kontener ([Watchtower](https://containrrr.dev/watchtower/)). To wygodne, ale niebezpieczne przy migracjach bazy i niezgodności wersji agenta. Uptime Kuma wymaga ręcznego `docker compose pull` ([wiki](https://github.com/louislam/uptime-kuma/wiki/%F0%9F%86%99-How-to-Update)); Checkly zaleca regularną aktualizację agenta ([Checkly](https://www.checklyhq.com/docs/private-locations/)); Datadog opisuje ręczną aktualizację obrazu/MSI ([Datadog](https://docs.datadoghq.com/synthetics/private_locations/)). Wniosek: ręczna aktualizacja + powiadomienie, auto-update allen opcjonalnie i z ostrzeżeniem.

### Jak robią inni

| Projekt / produkt | Serwer | Agent | Dystrybucja | Wydania i aktualizacje | Link |
|---|---|---|---|---|---|
| Uptime Kuma | Node.js, Vue 3 | brak; checki z procesu | obraz Docker, compose, Node + pm2 | tag `:2`, ręczny pull; GitHub Releases | [repo](https://github.com/louislam/uptime-kuma) |
| OpenStatus | Next.js, Hono, Go checker | Go, private-location 8,5 MB | compose, Coolify, Railway, osobne obrazy | AGPL-3.0, obraz per usługa; aktualizacja obrazów | [repo](https://github.com/openstatusHQ/openstatus) |
| Gatus | Go | brak; external endpoints przyjmują push | obraz `scratch` 26 MB, binarka, Helm | tag `stable`, GitHub Releases, reload configu | [repo](https://github.com/TwiN/gatus) |
| Healthchecks | Python 3.12+, Django | brak; pingi HTTP/e-mail | obraz Docker, pip/gunicorn, trzy procesy | obraz per release, migracje na starcie | [repo](https://github.com/healthchecks/healthchecks) |
| Checkmate | Node.js, React, MongoDB | Capture w Go (opcjonalny) | all-in-one obraz + MongoDB, compose, Helm | v4 all-in-one, tryb workera, telemetria | [repo](https://github.com/bluewave-labs/checkmate) |
| Statping-ng | Go | brak | binarka ~17 MB, obraz ~20 MB, compose | GitHub Releases, binarki per OS | [repo](https://github.com/statping-ng/statping-ng) |
| Upptime | GitHub Actions + Pages, Svelte | brak; Actions jest monitorem | repo + Pages, bez serwera | workflow co 5 min, zmiany przez git | [repo](https://github.com/upptime/upptime) |
| Checkly | SaaS, zamknięty (niepotwierdzone) | kontener `checkly/agent` (~1,5 GB z przeglądarkami) | Docker, Kubernetes | agent aktualizowany regularnie ręcznie | [docs](https://www.checklyhq.com/docs/private-locations/) |
| Better Stack | SaaS, zamknięty; hosted (brak self-host w dokumentacji) | brak agenta; proxy Squid do sieci prywatnych | tylko hosted | ciągłe wdrożenia SaaS | [docs](https://betterstack.com/docs/uptime/monitoring-private-networks/) |
| Datadog Synthetics | SaaS, zamknięty (niepotwierdzone) | worker Docker / usługa Windows MSI / Helm | Docker, Compose, K8s, ECS/Fargate, Helm | obraz v1.75.0, ręczna aktualizacja | [docs](https://docs.datadoghq.com/synthetics/private_locations/) |

## Porównanie

### Agent: kryteria KISS

| Kryterium | Go | Java + jlink | Java + GraalVM native | Node.js |
|---|---|---|---|---|
| Artefakt | 10–30 MB (szac.; Statping 17 MB, Gatus 26 MB) | runtime 23 MB + JAR (szac.) | 60–120 MB (szac.) | obraz 150 MB+ (Kuma 156 MB) |
| RAM w spoczynku | 10–40 MB (szac.) | 80–250 MB (szac.) | 30–80 MB (szac.) | 50–150 MB (szac.) |
| Instalacja | binarka, brak zależności | runtime lub native | binarka | Node + zależności |
| Cross-compile | trywialny | średni (jlink per OS) | trudny (toolchain per OS) | średni |
| Uprawnienia sieciowe | pełna kontrola DNS/TLS/ICMP | dobra, ale JVM | dobra | dobra |
| Nowy toolchain | tak | nie | tak (GraalVM) | tak |
| Ryzyko na 10 lat | niskie | niskie | średnie (refleksja, build) | średnie (node_modules) |

### Proces i budowanie

| Decyzja | Opcja minimalna | Opcja pośrednia | Opcja maksymalna | KISS |
|---|---|---|---|---|
| Role | jeden proces, `app.roles` | jedna aplikacja, wiele kontenerów | mikroserwisy + broker | A |
| Build | Maven + wrapper | Gradle + wrapper | własne skrypty/Bazel | A |
| Repo | monorepo, jeden tag | monorepo, tagi per artefakt | wiele repo | A |
| CI | GitHub Actions, 1 pipeline | macierz OS/JDK/Go | self-hosted runner | A |
| Dystrybucja | obraz + compose | + binarka agenta | + deb/rpm + Helm | A/B |
| Wydania | SemVer + changelog + podpisy | + auto-update opcjonalny | auto-update domyślnie | A |

### Koszt CI (szacunek)

| Wariant | PR-y/mies. | Czas PR | Minuty/mies. | Koszt prywatne | Koszt publiczne |
|---|---:|---:|---:|---:|---:|
| Minimalny | 20 | 10 min | 200 | ~1,2 USD | 0 USD |
| Rozszerzony | 20 | 25 min | 500 | ~3,0 USD | 0 USD |
| Z macierzą 3 OS | 20 | 45 min | 900 | ~5,4 USD | 0 USD |

## Własna implementacja

Warto napisać samemu: konfigurację ról i warunkowe beany (150–300 linii, 2–3 dni), endpoint wersji i stanu systemu (100–200 linii, 1–2 dni), sprawdzanie nowej wersji przez GitHub Releases API (80–150 linii, 1 dzień), pliki compose i dokumentację instalacji (100–200 linii, 1–2 dni), workflow CI i wydania (150–300 linii YAML, 3–5 dni), konfigurację GoReleaser/cosign (150–300 linii, 3–5 dni). Razem około 700–1 400 linii i 10–18 dni jednej osoby, bez UI i testów.

Nie pisać samemu: klienta HTTP, TLS, kryptografii podpisów, sterowników baz, parserów JSON/YAML, silnika migracji. To obszary wrażliwe, gdzie biblioteka z długim wsparciem jest tańsza niż własne koło.

Trudne miejsca: wieloarchitekturowe obrazy (amd64/arm64), keyless signing w CI, odtwarzalność binarek, Testcontainers na runnerze GitHub, migracje przy aktualizacji, bufor agenta przy braku łączności i zgodność protokołu N-1. Helm chart to osobne 300–500 linii i 5–8 dni; poza MVP.

## Rekomendacja

Rekomendacje do decyzji właściciela, nie decyzje:

1. **Agent: Go.** Najmniejszy artefakt i RAM, najlepsza kontrola nad siecią, przykłady Gatus/Statping/OpenStatus. Utrzymać agenta jako mały, izolowany moduł; nie przenosić executora serwera do Go.
2. **Proces: jedna aplikacja z rolami.** Self-host domyślnie wszystkie role w jednym procesie; chmura może rozdzielić API/executor/alerty później. Nie budować mikroserwisów na start.
3. **Build: Maven + Maven Wrapper.** Stabilny, dobrze wspierany przez Spring Boot; Gradle rozważyć tylko, jeśli build Mavena okaże się wąskim gardłem po pomiarze.
4. **Repozytorium: monorepo.** `server/`, `agent/`, `web/`, `deploy/`, `docs/`; agent jako osobny moduł Go; jeden tag `vX.Y.Z`; CI filtrowane po ścieżkach.
5. **CI: GitHub Actions, minimalny pipeline.** Unit testy, integracja HSQLDB + PostgreSQL/Testcontainers, Liquibase na obu bazach, testy agenta i frontendu; obrazy i wydania tylko na tag. Publiczne repo = 0 USD za standardowe runnery.
6. **Dystrybucja: jeden obraz serwera + compose, agent jako binarka i osobny obraz.** HSQLDB w pliku domyślnie, profil Postgres; deb/rpm przez nfpm po pierwszych użytkownikach; Helm dopiero po realnym popycie na K8s.
7. **Wydania: SemVer + Keep a Changelog + podpisane obrazy/binarki + powiadomienie, bez auto-update.** Cosign keyless dla obrazów, `SHA256SUMS` i podpis blobów dla binarek; ręczna aktualizacja według procedury.

Łączna lista zależności runtime: bez nowych zależności serwera poza obecnym stackiem; agent Go — stdlib + `golang.org/x/net/icmp` (zgodnie z net-go); narzędzia deweloperskie/CI — GoReleaser, nfpm, cosign, Docker/Buildx, Testcontainers. GoReleaser, nfpm i cosign nie trafiają do runtime.

## Ryzyka

| Ryzyko | Mitygacja |
|---|---|
| Drugi toolchain i dryf agent/serwer | kontrakt JSON, wspólne wektory parity, testy w CI, wspólny tag |
| CI zbyt wolne lub drogie | path filters, cache Mavena/Go, jeden pipeline na PR, macierz tylko na tag |
| Keyless signing zależny od publicznego Sigstore | dokumentacja weryfikacji offline; alternatywnie klucz długotrwały |
| Tylko Docker odstrasza użytkowników bare metal | binarka serwera + systemd unit, paczki deb/rpm później |
| Brak Helm w MVP | surowe manifesty lub dokumentacja K8s; chart po zgłoszeniach |
| Ręczne aktualizacje zostawiają stare agenty | powiadomienie w UI, wsparcie N-1, czytelny komunikat o zgodności |
| Auto-update przez Watchtower psuje migracje | nie włączać domyślnie; ostrzeżenie i backup przed migracją |
| Monorepo uruchamia wszystkie joby | `paths:` per workflow i wymagane checki tylko dla zmienionych obszarów |
| Rozjazd wersji serwer/agent | numer protokołu, testy zgodności, okno wsparcia |
| Maven wolniejszy niż Gradle | pomiar po etapie 0; migracja tylko przy realnym problemie |

## Pytania do grill-me

1. Czy agent może być osobnym artefaktem w tym samym repo i mieć ten sam numer wersji co serwer, czy chcesz niezależne wersjonowanie agenta?
2. Czy w MVP wystarczy obraz Docker + compose + binarka agenta, a deb/rpm i Helm odkładamy do czasu pierwszych użytkowników?
3. Czy repozytorium ma być publiczne od startu (darmowe Actions, szersza weryfikacja) czy prywatne do pierwszego wydania?
4. Czy akceptujesz keyless cosign (publiczny Sigstore), czy wymagasz własnego klucza i weryfikacji offline?
5. Czy update check może domyślnie odpytywać GitHub Releases API, czy ma być wyłączony w self-host i włączany świadomie?
6. Czy chmura ma od początku rozdzielać role API/executor/alerty, czy wystarczy jeden proces z możliwością podziału konfiguracją?
7. Czy Maven jest akceptowalny mimo wolniejszego builda, czy czas builda jest na tyle ważny, że rozważymy Gradle?
8. Czy monorepo ma objąć frontend i agenta, czy agent zostaje osobno ze względu na licencję, CVE lub tempo wydań?
9. Jak długo wspierasz starsze agenty (N-1, N-2) i czy serwer ma blokować połączenie z agentem spoza okna zgodności?
10. Czy potrzebujesz paczek deb/rpm przed pierwszym wydaniem, czy wystarczy binarka + systemd i obraz Docker?

## Źródła

- https://github.com/louislam/uptime-kuma
- https://github.com/louislam/uptime-kuma/wiki/%F0%9F%86%99-How-to-Update
- https://github.com/TwiN/gatus
- https://github.com/TwiN/gatus/blob/master/Dockerfile
- https://github.com/healthchecks/healthchecks
- https://github.com/openstatusHQ/openstatus
- https://github.com/bluewave-labs/checkmate
- https://github.com/bluewave-labs/capture
- https://github.com/statping-ng/statping-ng
- https://github.com/upptime/upptime
- https://www.checklyhq.com/docs/private-locations/
- https://betterstack.com/docs/uptime/monitoring-private-networks/
- https://docs.datadoghq.com/synthetics/private_locations/
- https://docs.oracle.com/en/java/javase/25/docs/specs/man/jlink.html
- https://www.graalvm.org/latest/reference-manual/native-image/
- https://maven.apache.org/wrapper/
- https://docs.gradle.org/current/userguide/gradle_wrapper.html
- https://go.dev/ref/mod
- https://docs.github.com/en/billing/managing-billing-for-your-products/about-billing-for-github-actions
- https://docs.github.com/en/rest/releases/releases
- https://docs.docker.com/compose/
- https://docs.sigstore.dev/cosign/signing/overview/
- https://docs.sigstore.dev/cosign/signing/signing_with_blobs/
- https://goreleaser.com/customization/nfpm/
- https://helm.sh/docs/topics/charts/
- https://semver.org/
- https://keepachangelog.com/en/1.1.0/
- https://containrrr.dev/watchtower/
- https://hub.docker.com/r/twinproduction/gatus
- https://hub.docker.com/r/louislam/uptime-kuma
- https://hub.docker.com/r/healthchecks/healthchecks
- https://hub.docker.com/r/checkly/agent
- https://gatus.io
