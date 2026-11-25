# 0006 — Kontrakt HTTP API, uwierzytelnianie i klient frontendu

- Status: accepted
- Date: 2026-11-21
- Superseded-by: none

## Context

Spec ([product-features.md](../specs/product-features.md), sekcje 0, 8 i 9) zakłada, że:
- każda akcja z UI jest dostępna w API, a UI jest tylko jednym z jego klientów (API-first);
- dokumentacja API jest generowana z kodu;
- listy są stronicowane i filtrowane po stronie serwera;
- limit żądań zwraca 429 z czasem ponowienia;
- tokeny API mają zakres i datę ważności.

Ogólne standardy właściciela (backend/frontend) wymagają dodatkowo:
- DTO i walidacji na granicach;
- błędów bez szczegółów implementacji;
- autoryzacji domyślnie odmawiającej;
- typów generowanych z OpenAPI, Zoda do walidacji w czasie działania i wykrywania dryfu kontraktu;
- jednego transportu HTTP;
- braku ponawiania mutacji bez kontraktu idempotencji.

Architektura serwera: [0005-server-architecture.md](0005-server-architecture.md). Decyzje zapadły w grill-me 2026-11-21.

Jak robią inni:
- **Healthchecks:** `/api/v1`–`/api/v3`, sesje Django, pingi na osobnej domenie.
- **OpenStatus:** schemat w kodzie (`@hono/zod-openapi`), sesja w ciasteczku.
- **Checkmate:** `/api/v1` i JWT.
- **Gatus:** `/api/v1`, OIDC.
- **Uptime Kuma:** UI przez Socket.IO, przez co API od lat jest niepełne.

## Decision

### Styl i wersjonowanie
- REST zasobowy. Akcje niebędące CRUD to podzasoby, np. `POST /checks/{id}/pause` i `POST /checks/{id}/run`.
- Wersja w ścieżce `/api/v1`. W ramach wersji tylko zmiany addytywne, a klient ignoruje nieznane pola.
- Zmiana łamiąca oznacza `v2`. Stara wersja działa równolegle przez okno wycofania z nagłówkami `Deprecation` i `Sunset` (RFC 9745, RFC 8594).
- API-first oznacza, że UI używa wyłącznie publicznego API, bez ukrytych endpointów. Przy nowej funkcji najpierw powstaje kontroler z DTO bez logiki, review obejmuje diff kontraktu, a dopiero potem implementację.

### Kontrakt OpenAPI
- Kontrakt powstaje z kodu (code-first) przez **springdoc-openapi**: kontrolery, rekordy DTO i adnotacje Bean Validation.
- Dwa kontrakty w repo:
  - **publiczny**: `/api/v1`, `/ping`, `/public/v1`; z niego korzystają integratorzy i frontend;
  - **agenta**: `/agent/v1`, wewnętrzny, dla repozytorium agenta.
- Test integracyjny (MockMvc) pobiera dokumenty OpenAPI i porównuje je z plikami w repo. Każda różnica to czerwony build, a zmiana API jest widoczna w diffie.
- Każda operacja ma unikalne, stabilne `operationId`.

### Błędy i walidacja
- Każdy błąd ma format **RFC 9457 Problem Details** (`application/problem+json`, Springowy `ProblemDetail`) z rozszerzeniami:
  - `code`: stabilny kod maszynowy;
  - `errors[]`: lista `{ pointer, code, params }` dla pól.
- Frontend tłumaczy `code` i `params` na PL/EN. Pole `detail` jest po angielsku, dla użytkowników API.
- Statusy:
  - 400: walidacja;
  - 401: brak uwierzytelnienia;
  - 403: brak uprawnienia;
  - 404: także zasób z cudzej przestrzeni;
  - 409: konflikt wersji;
  - 429: limit.
- Jeden globalny `@RestControllerAdvice`. Odpowiedź nigdy nie zawiera stack trace'ów, nazw hostów ani SQL.
- **Bean Validation** (Hibernate Validator) sprawdza kształt danych, a ograniczenia trafiają do OpenAPI. Reguły biznesowe (granice planu, weryfikacja celu) leżą w domenie i zwracają błąd w tym samym formacie.

### Stronicowanie i filtrowanie
- **Wszędzie kursor (keyset).** Odpowiedź ma postać `{ items, nextCursor }`. Kursor to nieprzezroczysty token z wartością sortowania (bez `id`; zakodowany base64url; maksymalna długość 256, zły kursor = 400 `invalid_cursor`).
- Nie ma sumy wyników ani skoku do strony N. Liczniki (np. checki według stanu) zwraca osobny endpoint podsumowania.
- Filtry to jawne parametry per endpoint. Sortowanie tylko po polach z białej listy (`?sort=name,asc`). Bez RSQL i OData.
- Maksymalny rozmiar strony jest konfigurowalny na poziomie instancji, domyślnie 100.
- Odpowiedzi budujemy z własnych rekordów DTO, a nie ze Springowego `Page`.

### Uwierzytelnianie
- Logowanie obsługuje nasz serwer przez Spring Security `oauth2-client`. Przeglądarka dostaje tylko ciasteczko sesji (HttpOnly, Secure, SameSite=Lax), a frontend nic nie wie o OIDC.
- Ochrona CSRF działa tak: Spring ustawia ciasteczko `XSRF-TOKEN`, a frontend odsyła nagłówek `X-XSRF-TOKEN`.
- Sesje są w bazie (**Spring Session JDBC**), nie w pamięci Tomcata. Przeżywają restart, działają przy HA i dają listę sesji oraz „wyloguj wszędzie”.
- **Sposoby logowania w MVP: tylko dostawcy zewnętrzni.**
  - self-host: dowolny dostawca OIDC;
  - chmura: GitHub, Google i własne OIDC przestrzeni.
  - Bez kont z hasłem, 2FA i linku w emailu w MVP.
- **Tokeny API** są nieprzezroczyste:
  - postać: `wk_` + 32 losowe bajty, przesyłane jako `Authorization: Bearer`;
  - w bazie tylko skrót SHA-256, zakres (odczyt/zapis), data ważności i ostatnie użycie;
  - nie są JWT;
  - nie wymagają CSRF.
- Odrzucony wariant: frontend sam prowadzi OIDC i wysyła JWT do bezstanowego serwera. Odpada, bo GitHub (OAuth2 bez OIDC, wymiana kodu wymaga sekretu) nie zadziała, Google daje JWT tylko jako ID token, a token w JavaScripcie jest dostępny dla XSS.

### Konfiguracja bezpieczeństwa (2026-11-22)

Trzy łańcuchy `SecurityFilterChain`:

- **bezstanowy** (`@Order(1)`) dla `/agent/v1/**`, `/ping/**`, `/public/v1/**`: `SessionCreationPolicy.STATELESS`, bez CSRF i bez request cache; każde żądanie wymaga uwierzytelnienia, odpowiedź to 401, łańcuch nigdy nie tworzy sesji. CSRF jest tu wyłączony, bo łańcuch nie używa sesji ani ciasteczek, więc przeglądarka nie dokleja uwierzytelnienia; test `statelessSurfaceIgnoresTheSessionCookie` dowodzi, że ciasteczko sesji nie jest czytane. Mechanizmy tokenów dochodzą w kolejnych zadaniach.
- **użytkownika** (`@Order(2)`) dla `/api/v1/**` oraz `/oauth2/**`, `/login/**`, `/logout`: `oauth2Login` włączany tylko wtedy, gdy istnieje `ClientRegistrationRepository` (bez dostawcy aplikacja wstaje bez logowania), `csrf.spa()` z ciasteczkiem `XSRF-TOKEN` i nagłówkiem `X-XSRF-TOKEN`, a `/api/v1/**` bez uwierzytelnienia dostaje 401 zamiast przekierowania.
- **domyślny** (`@Order(3)`): `/healthz` publiczne, wszystko inne `denyAll`; anonimowy dostaje 401.

OIDC: rejestracja ma identyfikator `oidc` (`/oauth2/authorization/oidc`) i jest konfigurowana wyłącznie właściwościami Springa (`spring.security.oauth2.client.registration.oidc.*`, `...provider.oidc.*`; w produkcji przez `SPRING_SECURITY_OAUTH2_CLIENT_...`). W `application.yml` nie ma wartości dostawcy. PKCE jest włączone jawnie przez `ClientSettings.requireProofKey(true)` w customizerze rejestracji, bo Boot 4.2 nie ma właściwości `require-proof-key`.

Rola Admin: bean `GrantedAuthoritiesMapper` czyta claim `wakey.security.admin-claim` (domyślnie `groups`) i dodaje `ROLE_ADMIN` tylko wtedy, gdy lista zawiera `wakey.security.admin-group` (pusta wartość oznacza, że nikt nie jest Adminem). Bez JMESPath i bez własnego `OidcUserService`.

Sesje: Spring Session JDBC z `spring.session.jdbc.initialize-schema: never`; tabele `SPRING_SESSION` i `SPRING_SESSION_ATTRIBUTES` tworzy changeset Liquibase `0003-spring-session.xml` (forward-only, bez `dbms`, tylko PostgreSQL — [0008](0008-postgresql-only.md)). Ciasteczko `SESSION` ma `HttpOnly`, `Secure` i `SameSite=Lax` (`server.servlet.session.cookie.*`). Ochrona przed session fixation zostaje domyślna (`changeSessionId`).

Ochrona logowania: PKCE, parametry `state` i `nonce` oraz weryfikacja podpisu i wystawcy ID tokenu pochodzą ze Spring Security. Po sukcesie serwer zawsze przekierowuje na `wakey.security.frontend-url` (domyślnie `/`); nie używa adresu z parametru żądania ani zapisanego żądania, bo request cache jest wyłączony, a handler sukcesu ma `alwaysUse=true`. Po błędzie (zły `state`, odrzucony kod, błąd dostawcy) przekierowuje na `{frontend-url}?login=failed` z jednym stałym znacznikiem, bez szczegółów błędu w adresie i odpowiedzi; sesję unieważnia, a w logu zostaje WARN z typem wyjątku bez tokenów i sekretów.

Limity sesji: bezczynność wyznacza `spring.session.timeout` (30m), a bezwzględny maksymalny wiek `wakey.security.session-max-age` (12h). Spring Session 4.2 nie ma gotowego mechanizmu bezwzględnego wieku, więc mały filtr przed wczytaniem kontekstu bezpieczeństwa unieważnia starszą sesję; żądanie API dostaje wtedy 401, a użytkownik loguje się ponownie i grupy są sprawdzane w IdP od nowa. Bez sekretów i danych sesji w logach.

Nagłówki na wszystkich łańcuchach: domyślne Springa plus `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `Referrer-Policy: no-referrer` i `Permissions-Policy: camera=(), microphone=(), geolocation=()`.

Wylogowanie: tylko `POST /logout` z tokenem CSRF. Strona logowania jest ustawiona jawnie na `/oauth2/authorization/oidc` (`oauth2Login.loginPage`), więc Spring nie generuje HTML-owych stron logowania ani wylogowania, a `GET /logout` nie jest obsługiwany. Odpowiedź ma `Clear-Site-Data: "cookies"` (`HeaderWriterLogoutHandler` + `ClearSiteDataHeaderWriter`; przeglądarki honorują je tylko po HTTPS) i status 200 bez przekierowania (`HttpStatusReturningLogoutSuccessHandler`). Odłożone: RP-initiated logout (`OidcClientInitiatedLogoutSuccessHandler`) i back-channel logout. Back-channel wymaga rejestru sesji OIDC, który domyślnie żyje w pamięci, a aplikacja ma działać na kilku instancjach; RP-initiated wymaga ustalonego end-session dostawcy i wróci razem z listą sesji i „wyloguj wszędzie”.

Logi: INFO przy udanym logowaniu (podmiot i informacja, czy Admin) oraz przy wylogowaniu, WARN przy nieudanym logowaniu. Źródłem są zdarzenia Spring Security (`AuthenticationSuccessEvent`, `AbstractAuthenticationFailureEvent`, `LogoutSuccessEvent`); logi nie zawierają tokenów ani sekretów.

Odpowiedzi 401 i 403 są logowane: 401 na INFO z powodem `missing-authentication`, a 403 na WARN z powodem z zamkniętej listy (`csrf-token-missing`, `csrf-token-invalid`, `access-denied`). W logu jest metoda i ścieżka żądania; przed wstrzyknięciem linii logu chroni format logu opisany w [0005-server-architecture.md](0005-server-architecture.md) (Logback `%replace` lokalnie, JSON ECS w obrazie), a nie własny sanitizer. Bez nagłówków, ciasteczek, tokenów, parametrów zapytania i treści żądania.

Na etap 1 przechodzą: limit prób logowania za portem `RateLimiter`, unieważnianie sesji po zmianie roli albo usunięciu z przestrzeni oraz „wyloguj wszędzie”.

### Role i dostęp
Decyzja z 2026-11-22. Trzy role w dwóch poziomach:

| Rola | Zakres | Co może | Skąd |
|---|---|---|---|
| **Admin** | cała aplikacja | wszystko: przestrzenie, plany, użytkownicy, nadużycia, stan systemu, każda przestrzeń | grupa z claimu dostawcy OIDC |
| **Edytor** | swoja przestrzeń (klient) | wszystko w przestrzeni: checki, kanały, incydenty, status pages, tokeny, zaproszenia do tej przestrzeni | zaproszenie od Admina albo Edytora tej przestrzeni |
| **Podgląd** | swoja przestrzeń | tylko odczyt | zaproszenie |

- **Przestrzeń to jedyna granica dostępu** (jak workspace w OpenStatus i projekt w Healthchecks).
  - Użytkownik widzi tylko przestrzenie, w których jest członkiem; cudza przestrzeń zwraca 404.
  - Status pages, checki, kanały i tokeny należą do przestrzeni.
  - Plan jest przypisany do przestrzeni, a jego limity (np. liczba status pages) liczą się per przestrzeń.
  - Bez uprawnień do pojedynczego checka. Kto chce rozdzielić dostęp, tworzy drugą przestrzeń.
- **Admin pochodzi z OIDC**, wzorem `role_attribute_path` z Grafany w najprostszej formie:
  - konfiguracja instancji podaje nazwę claimu (domyślnie `groups`) i nazwę grupy Admina;
  - bez JMESPath i bez nowej biblioteki; w kodzie jeden `GrantedAuthoritiesMapper` albo własny `OidcUserService`;
  - rola liczy się przy każdym logowaniu, a IdP jest źródłem prawdy. Adminów może być kilku, co chroni przed utratą dostępu;
  - wymaga IdP, który wysyła grupy (Authelia, Keycloak, Authentik, Pocket ID, Entra). GitHub i Google ich nie wysyłają, więc w chmurze Admin loguje się przez własnego dostawcę OIDC.
- **Role w przestrzeniach z zaproszeń albo przez API** (członek dodany po emailu działa jak zaproszenie przyjęte z góry). Mapowanie grup SSO na role w przestrzeniach to v1.
- Użytkownik, który nie jest Adminem i nie ma zaproszenia, nie ma dostępu do żadnej przestrzeni (domyślnie odmowa, jak `role_attribute_strict` w Grafanie).
- **Zakres tokenu API nie przekracza roli właściciela** (wzorem Healthchecks). Podgląd tworzy tylko tokeny do odczytu, a obniżenie roli ogranicza też tokeny tego użytkownika.
- Odrzucone:
  - osobna rola Admin w przestrzeni obok Admina aplikacji (za dużo poziomów);
  - lista emaili Adminów w konfiguracji (drugi mechanizm obok IdP);
  - „pierwszy zalogowany zostaje adminem” jak w Uptime Kuma (wyścig o przejęcie wystawionej instancji);
  - plan per konto z wieloma przestrzeniami jak w Healthchecks (do ewentualnego rozważenia przy chmurze w etapie 9).

### Plany, stałe klucze i GitOps (2026-11-24)

- **Plan to pakiet limitów przypisany do przestrzeni**, nie do użytkownika ani roli. Rola mówi, co ktoś może zrobić, a plan, ile tego może być. Plany definiuje konfiguracja instancji (`wakey.plans.<nazwa>.*`), jeden jest domyślny dla nowych przestrzeni; Admin przypisuje plan przez API. Ekran planów i zużycie limitów przychodzą w etapie 9.
- **API-first:** każdą zmianę da się zrobić przez API. Obiekty, do których odwołuje się skrypt albo plik, mają stały, czytelny klucz (slug przestrzeni, nazwa planu), a `PUT` po tym kluczu jest idempotentny.
- **Plik z etapu 7 to cienka warstwa:** `apply` liczy różnicę i woła te same serwisy co API, więc walidacja, limity planu i audyt są w jednym miejscu. Plik obejmuje przestrzenie i ich plan.
- **Użytkowników i członkostwa nie ma w pliku:** żadnych kont, loginów ani haseł w Gicie, bo to zbyt ruchoma część. Z dostępu plik może zawierać tylko mapowanie grup OIDC na role w przestrzeni, gdy ta funkcja powstanie (v1).

### Powierzchnie HTTP
| Prefiks | Klient | Uwierzytelnianie |
|---|---|---|
| `/api/v1/**` (w tym SSE `/api/v1/stream`) | UI, skrypty, IaC | sesja + CSRF albo token API |
| `/agent/v1/**` | agent Go (wersjonowany osobno) | token agenta `wka_…` |
| `/ping/<token>` | cron, skrypty (GET, POST, HEAD) | token w ścieżce. Bez wersji, bo URL-e lądują w cronach na zawsze. |
| `/public/v1/**` | status page, widżety | brak; snapshot w pamięci, nagłówki cache |

- Dwa `SecurityFilterChain`, oba domyślnie odmawiające:
  - **użytkownik**: `/api/v1/**`;
  - **bezstanowy**: `/agent/v1/**`, `/ping/**`, `/public/v1/**`. Ten łańcuch nigdy nie tworzy sesji i nie używa CSRF.
- Prefiksy tokenów (`wk_`, `wka_`) nie są wymienne.
- Prefiksy powierzchni i ścieżki logowania OIDC są w jednym miejscu (`dev.wakey.web.ApiPaths`), a każdy moduł dokłada własne ścieżki pod prefiksem. Testy kontraktu zostają przy literałach, żeby zmiana ścieżki w kodzie je wywróciła.
- Każda powierzchnia ma test bezpieczeństwa: brak uwierzytelnienia i token złego typu dają 401.
- SSE każda instancja zasila z portu kolejki ([0005](0005-server-architecture.md)). Wznawianie działa przez `Last-Event-ID`.
- Transport agenta (WebSocket, SSE albo long-poll) rozstrzyga backlog pkt 8. Format publicznego JSON rozstrzyga pkt 7.

### Limity i idempotencja
- Limity żądań liczy aplikacja: własny token bucket za portem `RateLimiter`. Później może go zastąpić adapter Redis.
- Klucz limitu to przestrzeń albo token, plan i rodzaj limitu. **Wartości pochodzą z konfiguracji planu**, więc premium może mieć więcej.
- IP klienta pochodzi z wbudowanej obsługi nagłówków `Forwarded` w Springu z jawną listą zaufanych proxy. Reverse proxy jest opcjonalne i służy tylko do zgrubnej ochrony per IP.
- 429 zawsze ma nagłówek `Retry-After`.
- Idempotencja w wersji KISS:
  - frontend nie ponawia mutacji sam;
  - akcje są idempotentne z definicji (pauza spauzowanego checka daje 200 bez zmian);
  - aktualizacje niosą `version` (`@Version`) i przy konflikcie dostają 409;
  - bez `Idempotency-Key`.

### Klient API we frontendzie
Wzorzec:
- `openapi-typescript` plus własny skrypt `generate-api.mjs`. Skrypt generuje `contract.ts`, `schema.ts` i `types.ts` z aliasami per `operationId`.
- Tryb `--check` wykrywa dryf i jest podpięty pod `build`, `typecheck` i `test`.
- Schematy Zod piszemy ręcznie w `features/*/api/*.schemas.ts`. **Każdy schemat ma `satisfies z.ZodType<typ z kontraktu>`**, więc rozjazd z OpenAPI nie przejdzie kompilacji.
- `*.api.ts` odpowiada za transport i `schema.parse`, a `*.queries.ts` za klucze, cache i unieważnianie.
- Jest jeden klient **axios**. Jego interceptory obsługują:
  - 401 jako zdarzenie wygaśnięcia sesji;
  - normalizację błędów Problem Details;
  - anulowanie żądań.
- axios sam przepisuje ciasteczko `XSRF-TOKEN` do nagłówka `X-XSRF-TOKEN` (domyślne nazwy Springa). Nie ma Bearer ani `oidc-client-ts`.
- Daty obsługuje **Day.js** z wbudowanymi wtyczkami `utc` i `timezone` (strefa IANA przestrzeni).

### Zasady bezpieczeństwa z incydentów konkurencji
Decyzja z 2026-11-22. Każda zasada odpowiada na znany incydent w podobnym narzędziu. W nawiasie podany jest etap, w którym musi działać ([roadmap.md](../specs/roadmap.md)).

| Zasada | Etap | Na jaki incydent odpowiada |
|---|---|---|
| **Test wszystkich endpointów:** test wylicza endpointy z kontraktów OpenAPI i sprawdza, że bez uwierzytelnienia każdy zwraca 401. Wyjątek to jawna lista publicznych (`/ping`, `/public/v1`). Nowy endpoint bez reguły dostępu wywala build. | 0 | Uptime Kuma: endpoint z danymi o pingach bez sprawdzenia uprawnień ujawniał prywatne monitory ([CVE-2026-32230](https://advisories.gitlab.com/npm/uptime-kuma/CVE-2026-32230/)) |
| **Test flag ciasteczka sesji:** `HttpOnly`, `Secure` i `SameSite=Lax` są sprawdzane testem, nie tylko konfiguracją. | 0 | Gatus: ciasteczko sesji OIDC bez `Secure` ([GHSA-wwqf-6p54-3cfq](https://cve.circl.lu/vuln/ghsa-wwqf-6p54-3cfq)) |
| **Sesja nie przeżywa zmiany uprawnień:** <br>– maksymalny czas życia sesji, konfigurowalny, domyślnie 12 h, po którym następuje ponowne logowanie i ponowne sprawdzenie grupy w IdP; <br>– zmiana roli albo usunięcie z przestrzeni unieważnia sesje tego użytkownika (Spring Session JDBC, wyszukiwanie po użytkowniku). | 0–1 | Uptime Kuma: zmiana hasła nie unieważniała sesji ([CVE-2023-49804](https://advisories.gitlab.com/npm/uptime-kuma/CVE-2023-49804/)). U nas usunięcie z grupy w IdP zadziała dopiero przy kolejnym logowaniu. |
| **Sekrety w API tylko do zapisu:** odpowiedź pokazuje „ustawione” albo maskę, nigdy wartość (hasła i tokeny kanałów, klucze). Ustawienia instancji widzi tylko Admin. | 2 | Checkmate: zwykły użytkownik czytał sekrety aplikacji przez `/api/v1/settings` ([CVE-2025-48024](https://nvd.nist.gov/vuln/detail/CVE-2025-48024)) |
| **Zamknięty zestaw schematów celu:** <br>– checki przyjmują tylko `http`/`https` oraz jawne cele TCP i DNS; <br>– inne schematy (np. `file:`) są odrzucane przy zapisie z błędem walidacji. | 1 | Uptime Kuma: `file://` w monitorze „real browser” czytał lokalne pliki ([CVE-2024-56331](https://advisories.gitlab.com/npm/uptime-kuma/CVE-2024-56331/)) |
| **Jedna brama ruchu wychodzącego:** checki, webhooki, Pushover, email przez API i wywołania weryfikacji celów przechodzą przez tę samą politykę i przypięcie IP ([0003](0003-network-checks.md)). Webhook nie jest obejściem ochrony przed SSRF. | 1–2 | SSRF przez webhooki, np. [Grafana OnCall CVE-2024-5526](https://grafana.com/security/security-advisories/cve-2024-5526/) |
| **Szablony powiadomień bez logiki:** <br>– tylko podstawianie nazwanych pól (`{{check.name}}`) z escapowaniem zależnym od formatu (JSON, tekst); <br>– bez pętli, filtrów, include'ów i silnika szablonów z dostępem do plików lub kodu; <br>– trywialny, własny kod z testami złośliwych wejść. | 2 | Uptime Kuma: wstrzyknięcie kodu w szablonach powiadomień, które pozwalało czytać pliki ([GHSA-vffh-c9pq-4crh](https://advisories.gitlab.com/npm/uptime-kuma/GHSA-vffh-c9pq-4crh/)), z niepełną pierwszą poprawką ([CVE-2026-33130](https://www.tenable.com/cve/CVE-2026-33130)) |
| **Bez wtyczek ładowanych w czasie działania.** Nowy typ checka albo kanału to kod w repo, przechodzący przez review i CI. | stale | Uptime Kuma: wtyczka z RCE i path traversal w nazwie repozytorium wtyczek ([CVE-2023-36821](https://advisories.gitlab.com/npm/uptime-kuma/CVE-2023-36821/), [CVE-2023-36822](https://advisories.gitlab.com/npm/uptime-kuma/CVE-2023-36822/)) |

Pełny model zagrożeń to backlog pkt 10, w etapie 8. Te zasady są jego punktem startu.

## Consequences

- Nowe zależności serwera: springdoc-openapi, Bean Validation (`spring-boot-starter-validation`), `spring-boot-starter-security`, `spring-boot-starter-oauth2-client`, `spring-session-jdbc`. W bramce zgodności na etapie 0 trzeba sprawdzić linię springdoc dla Boot 4 i wersjonowanie API w Spring Framework 7. Baza to wyłącznie PostgreSQL ([0008](0008-postgresql-only.md)).
- Nowe zależności frontendu: zod, axios, dayjs, openapi-typescript (dev). Uzupełniają [0004-frontend.md](0004-frontend.md).
- Bez oasdiff zmianę łamiącą w `v1` wychwyci tylko review diffu kontraktu. To świadome ryzyko.
- Kursor wszędzie oznacza, że lista checków nie ma numerów stron. UI używa „wczytaj więcej” albo przewijania, a liczniki pochodzą z podsumowania.
- **Zmiana spec:** self-host potrzebuje dostawcy OIDC (np. Authelia, Pocket ID, Keycloak), więc zasada „self-host bez usług zewnętrznych do startu” ma ten wyjątek. Lokalny admin, 2FA i link w emailu przechodzą na później. Rola Admin z grup SSO wchodzi do MVP. Mapowanie grup na role w przestrzeniach zostaje w v1. Role upraszczają się do trzech: Admin (cała aplikacja), Edytor i Podgląd (przestrzeń).
- Limity per instancja: przy HA każda instancja liczy osobno, dopóki nie powstanie adapter Redis.

## Links

- [0005-server-architecture.md](0005-server-architecture.md), [0007-static-checks.md](0007-static-checks.md), [0004-frontend.md](0004-frontend.md), [0002-stack.md](0002-stack.md)
- [product-features.md](../specs/product-features.md)
