# Roadmapa etapów 0–9

Ten dokument mówi, **w jakiej kolejności** powstają funkcje z [product-features.md](product-features.md). „MVP” w spec oznacza całość etapów 0–9. Plan v0.16, z którego pochodziły pierwotne numery etapów, zaginął. Roadmapę ustalono od nowa 2026-11-21 tak, żeby zgadzała się z numerami etapów w backlogu.

## Reguły układania

1. **Najpierw to, co później jest drogie.** Elementy przekrojowe, które dotykają wszystkich tabel albo wszystkich zapisów, wchodzą do etapów 0–1 jako szkielet, nawet bez pełnych ekranów.
2. **Potem najkrótsza droga do działającej połowy produktu** (M1: checki, alert na telefon, heartbeaty).
3. **Danych nie da się odtworzyć wstecz.** To, co trzeba zbierać do późniejszych widoków (fazy, kubełki, przedziały stanu), zbieramy od pierwszego checka.
4. **Bezpieczeństwo jest od pierwszego żądania do celu użytkownika**, a nie dopiero przed publikacją.
5. **Co działa na własnym VPS, ma kopie zapasowe i watchdog od pierwszego dnia.**

## Etapy

| Etap | Zakres | Po etapie | Research przed etapem |
|---|---|---|---|
| **0. Fundament techniczny** | Szkielet repo (Maven + wrapper, Vite), moduły i reguły ArchUnit, Spotless, Error Prone + NullAway, Checkstyle, ESLint, minimalne CI na PostgreSQL i HSQLDB. Liquibase, lease z fencing tokenem, port kolejki z outboxem. Logowanie OIDC, sesje w bazie, wskazanie pierwszego Admina. Bramka zgodności wersji i spike'i: executor z 5 000 checków, przypięcie IP z SNI w Reactor Netty, lease na obu bazach. | Szkielet się buduje, loguje użytkownika i przechodzi wszystkie sprawdzenia statyczne. | — (ADR 0002–0007) |
| **1. Fundament domenowy i checki z serwera** | Przestrzenie (`space_id`, izolacja z 404), role (Admin, Edytor, Podgląd), dziennik audytu i historia zmian ze źródłem, trzy poziomy konfiguracji z wartością efektywną, szyfrowanie sekretów, wspólna polityka ponowień. Checki HTTP, TCP i certyfikat z serwera, z polityką ruchu wychodzącego i przypięciem IP. Wyniki z `location_id` (na start „serwer”), stan `UNKNOWN`, kategoria błędu. Przedziały stanu, kubełki faz, retencja surowych wyników. Panel: lista, dodanie, „Testuj”, „Sprawdź teraz”, szczegóły bez wykresów; odświeżanie przez odpytywanie. API `/api/v1` z tokenami. | Widać stan własnych checków w panelu i przez API. | 1.5 frontend, 2 sieć, 3 UI/UX |
| **2. Alerty, powiadomienia i własna instancja** | Kanały: email, webhook, Pushover. Polityki alertów, flapping, alert zbiorczy, limit powiadomień, martwy kanał, meta-alert. Zaproszenia. Obraz Dockera i compose. **Instancja na VPS w UE**: zrzuty bazy, kopia klucza sekretów poza VPS, test odtworzenia, zewnętrzny watchdog, podstawowy „stan systemu”. Powiadomienie o nowej wersji. | **Pierwszy alert na telefonie.** Właściciel używa wakey na co dzień. | 4 dostawcy w UE, 5 kanały push |
| **3. Heartbeaty i crony** | Wejście `/ping/<token>`, okres i cron z grace, strefa czasowa, kontrola wejścia, okres rozruchu, limit pingów (429), bufor pingów. Gotowe snippety. | **M1: połowa produktu** (odpowiednik Uptime Kuma + Healthchecks w self-hoście). | 6 semantyka heartbeatów |
| **4. Incydenty i planned work** | Incydenty automatyczne i ręczne, szkice, okna serwisowe, masowe wyciszenie, post-mortemy w Markdown. | Zdarzenia mają opis i komunikację. | — |
| **5. Status page, wykresy i raporty** | Publiczna status page (`/public/v1`, snapshot), SSE w panelu i na stronie. Wykresy faz per lokalizacja i historia (Chart.js), przegląd (dashboard), raporty. | Status page dla klientów. | 7 format publicznego JSON |
| **6. Agent i lokalizacje** | Agent Go (osobne repo), rejestracja, kanał `/agent/v1`, wiele lokalizacji, kworum, ICMP, wzorzec zbiorowego błędu. | Monitoring z wnętrza sieci i z wielu miejsc. | 8 kanał agenta przez proxy |
| **7. Plik konfiguracyjny** | Plik deklaratywny z wersją schematu, wykrywanie różnic, podgląd przed wgraniem, eksport, zachowanie przy wdrożeniu. | Konfiguracja w gicie. | 9 format pliku i importy |
| **8. Gotowość do publikacji** | Weryfikacja celów (chmura), zgłoszenia nadużyć, model zagrożeń i poprawki, pełny „stan systemu”, nazwa i znak towarowy, dokumenty prawne, tłumaczenie dokumentacji na angielski. | Gotowe do pokazania publicznie. | 10 zagrożenia, 11 nazwa, 12 prawo (w trakcie), 13 tłumaczenie |
| **9. Start** | Publikacja open source. Chmura: rejestracja (tryby, kody, lista oczekujących), plany i limity per plan, własne OIDC przestrzeni. | **MVP.** | 12 prawo zakończone |

## Zasady dla etapów

- Funkcja z oznaczeniem MVP w spec, której nie ma w tabeli, trafia do etapu swojej sekcji spec: checki do 1, alerty do 2, incydenty do 4, status page do 5, agent do 6, plik do 7, administracja chmury do 9.
- Element fundamentu z etapów 0–1 może mieć ekran później. Zapis do audytu działa od etapu 1, a przeglądarka dziennika może przyjść później.
- Etap jest skończony, gdy jego zakres działa na obu bazach i przechodzi wszystkie sprawdzenia statyczne.
- v1 i „później” w spec zaczynają się po etapie 9.

## Linki

- [product-features.md](product-features.md), [ui-ux.md](ui-ux.md)
- [backlog.md](../research/backlog.md)
- [0002-stack.md](../decisions/0002-stack.md), [0003-network-checks.md](../decisions/0003-network-checks.md), [0005-server-architecture.md](../decisions/0005-server-architecture.md), [0006-api-contract.md](../decisions/0006-api-contract.md), [0007-static-checks.md](../decisions/0007-static-checks.md)
