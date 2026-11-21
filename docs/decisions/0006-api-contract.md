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

Ogólne standardy właściciela (`ai/domains/backend/AGENTS.md`, `ai/domains/frontend/`) wymagają dodatkowo:
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
- **Wszędzie kursor (keyset).** Odpowiedź ma postać `{ items, nextCursor }`. Kursor to nieprzezroczysty token zawierający wartość sortowania i `id`. Warunek jest przenośny między bazami: `ts < ? OR (ts = ? AND id < ?)`.
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
Wzorzec jak w `chaos-view/apps/velma`:
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

## Consequences

- Nowe zależności serwera: springdoc-openapi, Bean Validation (`spring-boot-starter-validation`), `spring-boot-starter-security`, `spring-boot-starter-oauth2-client`, `spring-session-jdbc`. W bramce zgodności na etapie 0 trzeba sprawdzić linię springdoc dla Boot 4, schemat Spring Session JDBC dla HSQLDB i wersjonowanie API w Spring Framework 7.
- Nowe zależności frontendu: zod, axios, dayjs, openapi-typescript (dev). Uzupełniają [0004-frontend.md](0004-frontend.md).
- Bez oasdiff zmianę łamiącą w `v1` wychwyci tylko review diffu kontraktu. To świadome ryzyko.
- Kursor wszędzie oznacza, że lista checków nie ma numerów stron. UI używa „wczytaj więcej” albo przewijania, a liczniki pochodzą z podsumowania.
- **Zmiana spec:** self-host potrzebuje dostawcy OIDC (np. Authelia, Pocket ID, Keycloak), więc zasada „self-host bez usług zewnętrznych do startu” ma ten wyjątek. Lokalny admin, 2FA i link w emailu przechodzą na później. Do ustalenia przy etapie użytkowników: jak wskazać pierwszego Admina (np. email albo subject OIDC w konfiguracji instancji).
- Limity per instancja: przy HA każda instancja liczy osobno, dopóki nie powstanie adapter Redis.

## Links

- [0005-server-architecture.md](0005-server-architecture.md), [0007-static-checks.md](0007-static-checks.md), [0004-frontend.md](0004-frontend.md), [0002-stack.md](0002-stack.md)
- [product-features.md](../specs/product-features.md)
