# 0002 — Stack serwera, baz, agenta i dostarczania

- Status: accepted
- Date: 2026-11-21
- Superseded-by: none

Zastępuje [0001-stack.md](0001-stack.md). Klient checków i DNS: [0003-network-checks.md](0003-network-checks.md). Frontend: [0004-frontend.md](0004-frontend.md).

## Context

Właściciel otworzył ponownie pkt 1 backlogu: research do ADR 0001 nie porównywał stacku z istniejącymi monitorami i nie rozważał Springa jako „wrappera” nad asynchronicznym I/O. Nowy research ([stack-server.md](../research/stack-server.md), [stack-db.md](../research/stack-db.md), [stack-ops.md](../research/stack-ops.md)) porównał opcje z Uptime Kuma, OpenStatus, Gatus, Healthchecks, Checkmate i innymi. Decyzje zapadły w grill-me 2026-11-20/21.

Zasady właściciela, które rozstrzygały wybory:
- Rzeczy nietrywialne albo wrażliwe na bezpieczeństwo: sprawdzona biblioteka z długim wsparciem i dużą społecznością. Rzeczy trywialne: własny kod.
- Serwer maksymalnie wydajny, dopóki pozostaje łatwy w utrzymaniu dla jednej osoby.
- Projekt open source na lata, self-host bez usług zewnętrznych, chmura w UE.

## Decision

### Serwer
- Java 25 LTS, Spring Boot 4.x (najnowsza wspierana linia OSS; dokładna wersja po bramce zgodności na etapie 0), Spring MVC na Tomcacie z wątkami wirtualnymi. Bez WebFluxa.
- Checki wykonuje asynchroniczny klient Reactor Netty w warstwie executora; szczegóły w [0003](0003-network-checks.md). Reactor nie wychodzi poza tę warstwę.
- Jedna aplikacja z rolami włączanymi konfiguracją (`app.roles`): API, executor, alerty, agregacje. Moduły komunikują się przez interfejsy i bazę, bez współdzielonego stanu w pamięci, tak aby każdą rolę dało się uruchomić jako osobny proces samą konfiguracją. Szczegóły modułów, portów i komunikacji między rolami: [0005-server-architecture.md](0005-server-architecture.md).
- Build: Maven + Maven Wrapper.
- Obserwowalność: Spring Boot Actuator (health dla Dockera, tylko jawnie wystawione endpointy) i Micrometer jako jedyne API metryk od etapu 0. Eksport do OTLP (dodatkowa zależność i konfiguracja, bez zmian w kodzie) w etapie 8 ([roadmap.md](../specs/roadmap.md)).

### Bazy i dane
- PostgreSQL (chmura, większe instalacje) albo HSQLDB w trybie pliku i jednego procesu (self-host). Wspólny podzbiór SQL i testy na obu bazach, jak w ADR 0001 (sekcja „Wspólny podzbiór SQL” obowiązuje bez zmian).
- Dostęp: Spring Data JDBC dla konfiguracji i domeny, `JdbcClient` dla wyników, batchy i raportów. Bez JPA.
- Migracje: Liquibase XML, forward-only. Licencja Liquibase Community to FSL-1.1-ALv2 (nie OSI); zaakceptowana świadomie.
- Wyniki pomiarów:
  - tabela przedziałów stanu (check, lokalizacja, stan, od, do) — z niej uptime i pasek historii;
  - kubełki czasów odpowiedzi per faza (min/avg/p95/max) 5 min, 1 h, 1 dzień, retencja konfigurowalna w planie;
  - pełne wyniki z detalami tylko dla błędów i zmian stanu;
  - surowe wyniki OK w oknie czasowym (domyślnie 24 h, konfigurowalne), retencja batchowym `DELETE` po czasie.

### Lider i HA
- Własna tabela lease z fencing tokenem (projekt z ADR 0001: `leader_lock`, czas z bazy, TTL 30 s, odnowienie co 10 s). Gotowe mechanizmy (ShedLock, Spring Integration) nie mają fencingu.
- MVP: jedna instancja, kod już z dzierżawą.
- Chmura po MVP: instancje A/B/C; wszystkie obsługują API, UI i wyniki agentów; pracę w tle (harmonogram, alerty, agregacje) wykonuje lider, pozostałe czekają gotowe do przejęcia. Zadania idempotentne. Kolejka z konkurującymi konsumentami dopiero, gdy jeden lider nie wyrobi.

### Agent
- Go (stdlib + `golang.org/x/net`), statyczna binarka, Linux + Docker w MVP, aktualizacje ręczne.
- Osobne repozytorium agenta; kontrakt JSON i wspólne wektory testowe z serwerem.

### Dostarczanie
- Self-host: obraz Docker + docker compose (HSQLDB w pliku domyślnie, profil PostgreSQL). Bez Kubernetesa i Helma.
- Wydania bez podpisów w MVP.
- Powiadomienie o nowej wersji: serwer raz na dobę sprawdza GitHub Releases; włączone domyślnie, z wyłącznikiem.

## Consequences

- Dwa toolchainy (Java/Maven i Go) w dwóch repozytoriach; zmiana kontraktu agenta wymaga dwóch PR.
- Dwa modele wykonania w jednym procesie (wątki wirtualne i pętla Netty); callbacki klienta checków nie mogą blokować.
- HSQLDB: limit pliku `.data` 64 GB domyślnie, brak partycjonowania; retencja i kubełki muszą to uwzględnić.
- Liquibase na FSL: przy zmianie licencji albo warunków plan B to Flyway OSS.
- Brak podpisów wydań w MVP zmienia [product-features.md](../specs/product-features.md) (agent).
- Do zrobienia na etapie 0: bramka zgodności wersji (Boot 4.x, Liquibase, Testcontainers, Spring Data JDBC, Reactor Netty), spike executora z 5 000 checków, testy lease na obu bazach.
- Otwarte: CI (GitHub Actions, minimalny pipeline), kopie zapasowe i odtwarzanie, szczegóły agenta Go (format konfiguracji, bufor) — do ustalenia.

## Links

- [stack-server.md](../research/stack-server.md), [stack-db.md](../research/stack-db.md), [stack-ops.md](../research/stack-ops.md)
- [net-go.md](../research/net-go.md)
- [0001-stack.md](0001-stack.md)
