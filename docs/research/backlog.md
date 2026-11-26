# Research backlog: uptime monitor (v1.1)

Lista tematów do zbadania osobnym agentem albo z ekspertem, zanim dojdzie do danego etapu. Kolejność według tego, kiedy wynik jest potrzebny. Etapy (0–9): [roadmap.md](../specs/roadmap.md).

**Zmiany v1 → v1.1 (2026-11-19):** pkt 1 zamknięty decyzją w `docs/decisions/0001-stack.md`; dodany pkt 1.5 (frontend); pkt 2 rozszerzony o Go dla agenta.

| # | Temat | Kiedy potrzebny | Kto |
|---|---|---|---|
| 0 | Roadmapa etapów 0–9 | przed etapem 0 | zamknięte: `docs/specs/roadmap.md` |
| 1 | Stack Java, dwie bazy i agent | przed etapem 0 | zamknięte: `docs/decisions/0002-stack.md` (zastępuje 0001); otwarte w ADR: CI, backup, szczegóły agenta |
| 1.5 | Frontend: biblioteka komponentów, formularze, wykresy | przed etapem 1, wykresy przed 5 | zamknięte: `docs/decisions/0004-frontend.md` |
| 2 | Pomiary sieciowe w Javie (serwer) i Go (agent) | przed etapem 1 | serwer zamknięty: `docs/decisions/0003-network-checks.md`; agent: `docs/research/net-go.md`, szczegóły w repo agenta |
| 3 | UI/UX | przed etapem 1, najpóźniej przed 5 | agent (brief: `docs/research/ui-ux-brief.md`) |
| 4 | Dostawcy w UE (VPS, email, magazyn, watchdog) | przed etapem 2 | właściciel; decyzja w ADR (fala 2a), bez raportu |
| 5 | Kanały push i treść powiadomień | przed etapem 2 | właściciel; Pushover ze spec, treść w makietach 2b, ADR z fali 2a |
| 6 | Semantyka heartbeatów i cronów | przed etapem 3 | agent (fala 3a) |
| 7 | Format publicznego JSON status page | przed etapem 5 | agent |
| 8 | Kanał agenta przez firewalle i proxy | przed etapem 6 | agent |
| 9 | Format pliku konfiguracyjnego i importy | przed etapem 7 | agent |
| 10 | Model zagrożeń | przed etapem 8 | agent, potem przegląd człowieka |
| 11 | Nazwa projektu i znak towarowy | przed etapem 8 | agent |
| 12 | Prawo: regulamin, prywatność, DPA, kanały spoza UE | w trakcie etapu 8 | prawnik (agent może przygotować pytania) |
| 13 | Tłumaczenie dokumentacji na angielski | przed publikacją open source | agent |

---

## 1. Stack Java, dwie bazy i agent (przed etapem 0) — ZAMKNIĘTE
Decyzje: `docs/decisions/0002-stack.md` (zastępuje 0001), architektura serwera i API w `0005`–`0007`.

## 1.5. Frontend: biblioteka komponentów, formularze i wykresy (przed etapem 1, wykresy przed 5)
Technologie frontendu; projekt UI/UX pozostaje w pkt 3.
- **Biblioteka komponentów:** dostępność WCAG 2.2 AA, tryb jasny i ciemny, i18n PL/EN, dopasowanie do tokenów z `docs/specs/ui-ux.md`.
- **Formularze ze schematu:** deskryptor typów checków z backendu, progressive disclosure, źródło wartości i „Przywróć”, walidacja inline, zachowanie formularza przy weryfikacji.
- **Wykresy przy 5 000 checków:** preagregacja po stronie API, lekkie sparkline w liście, wykres faz per lokalizacja, zakresy 24 h–365 dni, zachowanie przy braku danych.
- **Publiczna status page:** ten sam frontend, osobny shell, snapshot i wiek danych przy awarii API, aktualizacje live (SSE z fallbackiem), stan filtrów w URL.
- **Testy:** dostępność (axe), wydajność listy i wykresów, kluczowe przepływy na telefonie.
- Wynik: rekomendacja bibliotek i wzorców z uzasadnieniem i listą ryzyk.

## 2. Pomiary sieciowe w Javie i Go (przed etapem 1)
### 2a. Java (serwer)
- **ICMP w Javie (serwer).** Czy serwer potrzebuje pingu bez roota; uprawnienia kontenera; zachowanie na Linuksie; zapasowa ścieżka przez TCP. Agent ma własny wariant w 2b.
- **Czasy w fazach** (DNS, połączenie, TLS, pierwszy bajt): czy standardowy klient HTTP w Javie je udostępnia, czy potrzebny jest niższy poziom.
- **Własne rozwiązywanie DNS z przypięciem adresu** (ochrona przed DNS rebinding w bramie ruchu wychodzącego), IPv6, zachowanie przy wielu adresach.
- Certyfikaty: pełny łańcuch, własne CA, wykrywanie braku odnowienia.
- Kategoria błędu: które wyjątki sieciowe oznaczają „cel nie odpowiada”, a które „lokalizacja nie może wyjść do sieci”.

### 2b. Go (agent)
- **ICMP bez roota:** `x/net/icmp` z sieciami `udp4`/`udp6`; zależność od `net.ipv4.ping_group_range`, w Dockerze `--sysctl` albo `CAP_NET_RAW`; fallback TCP, gdy jądro nie pozwala.
- **Fazy HTTP:** `net/http/httptrace` (DNS, połączenie, TLS, pierwszy bajt), własny `Dialer`/resolver, przypięcie adresu, łańcuch certyfikatów i własne CA.
- **Dystrybucja:** statyczna binarka `CGO_ENABLED=0` dla amd64/arm64, bez JRE; CA certyfikaty i tzdata w obrazie scratch; podpisane wydania, aktualizacja ręczna.
- **Docker:** `--network=host` dla pomiaru z sieci hosta; uprawnienia do ICMP; różnica między siecią kontenera a hosta.
- **Zgodność z serwerem:** wspólne wektory testowe dla asercji HTTP, kodów błędów i kategorii „cel nie odpowiada” / „lokalizacja nie może wyjść do sieci”.
- Wynik: rekomendacja implementacji agenta i lista ryzyk.

## 3. UI/UX (przed etapem 1, najpóźniej przed 5)
Osobny brief: `docs/research/ui-ux-brief.md`.

## 4. Dostawcy w UE (przed etapem 2)
- VPS: dwóch dostawców z UE z różnymi sieciami (pod przyszły drugi VPS), IPv6, ceny, polityka wobec ruchu monitorującego.
- Email transakcyjny z danymi w UE: dostarczalność, cena przy małym wolumenie, zachowanie przy awarii.
- Magazyn obiektów w UE na archiwum i zrzuty (osobno od VPS).
- Zewnętrzny watchdog dla sygnału „żyję”, niezależny od naszego VPS.
- Wynik: krótka lista z kosztami miesięcznymi na start.

## 5. Kanały push i treść powiadomień (przed etapem 2)
- Pushover: priorytety, alarmy krytyczne omijające tryb cichy, limity, potwierdzenie w aplikacji.
- ntfy (także self-host) jako kanał z danymi w UE.
- Co pokazuje zablokowany ekran telefonu i jak zmieścić decyzję w treści powiadomienia.
- Jakie dane trafiają poza UE przy każdym kanale (wsad dla prawnika, punkt 12).

## 6. Semantyka heartbeatów i cronów (przed etapem 3)
- Jak Healthchecks i inne narzędzia liczą „spóźniony” ping: cron, okres, grace, strefy czasowe, zmiana czasu.
- Przypadki brzegowe: start bez końca, długie zadania, równoległe uruchomienia, rozjechane zegary klientów.
- Gotowe snippety w popularnych środowiskach (bash, PowerShell, systemd, Kubernetes CronJob) z ograniczonymi ponowieniami.

## 7. Format publicznego JSON status page (przed etapem 5)
- Czy istnieje de facto standard formatu (np. format API popularnych hostowanych status page), który czytają agregatory, widżety i narzędzia.
- Zgodność z nim za darmo dałaby integracje bez pisania kodu.

## 8. Kanał agenta przez firewalle i proxy (przed etapem 6)
- Stałe połączenie (WebSocket, SSE) a długie odpytywanie: co przechodzi przez firmowe proxy, proxy z inspekcją TLS i proxy wymagające logowania.
- Wykrywanie, że stałe połączenie nie działa, i przejście na odpytywanie.
- Jak inne narzędzia z agentami (np. prywatne lokalizacje w narzędziach monitoringu, agenci CI) rozwiązują łączenie wychodzące.

## 9. Format pliku konfiguracyjnego i importy (przed etapem 7)
- Jak wyglądają pliki Gatus, eksport Uptime Kuma, API Healthchecks i providery infrastructure-as-code dla narzędzi monitoringu.
- Wzorce wersjonowania schematu i migracji plików.
- Mapowanie pojęć przy imporcie z Kuma (importer w v1).

## 10. Model zagrożeń (przed etapem 8)
- Punkt startu: zasady z incydentów konkurencji w `docs/decisions/0006-api-contract.md` (sekcja „Zasady bezpieczeństwa z incydentów konkurencji”).
- Agent (przejęty serwer, przejęty agent), SSRF i brama ruchu wychodzącego, weryfikacja celów, izolacja przestrzeni, link w emailu, zgłoszenia nadużyć.
- Lista kontrolna według uznanego standardu weryfikacji bezpieczeństwa aplikacji.
- Wynik: lista zagrożeń z obecnym zabezpieczeniem i lukami.

## 11. Nazwa projektu i znak towarowy (przed etapem 8)
- Do tego czasu „wakey” to nazwa robocza w kodzie (ADR 0002). Przy zmianie nazwy trzeba zmienić pakiet i `groupId` `dev.wakey`, prefiks `wakey.*`/`WAKEY_*`, nazwę obrazu i ewentualnie prefiksy tokenów.
- Wolna nazwa: GitHub, rejestry pakietów, domena, kolizje z istniejącymi narzędziami monitoringu.
- Wstępne sprawdzenie w bazach znaków towarowych UE.

## 12. Prawo (w trakcie etapu 8, przed etapem 9)
- Regulamin, polityka prywatności, umowa powierzenia (DPA).
- Kanały powiadomień spoza UE (Pushover, Telegram, Slack): czy i na jakich warunkach.
- Adresy subskrybentów i odbiorców alertów jako dane osobowe.
- Później: VAT w UE i faktury przy płatnościach.
- Agent może przygotować listę pytań do prawnika, ale decyzje podejmuje prawnik.
