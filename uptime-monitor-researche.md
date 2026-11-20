# Uptime monitor: researche do zrobienia osobno (v1)

Lista tematów, które warto zbadać osobnym agentem albo z ekspertem, zanim dojdzie do danego etapu (etapy według `uptime-monitor-plan.md` v0.16, sekcja 14). Kolejność według tego, kiedy wynik jest potrzebny.

| # | Temat | Kiedy potrzebny | Kto |
|---|---|---|---|
| 1 | Stack Java i dwie bazy | przed etapem 0 | agent |
| 2 | Pomiary sieciowe w Javie (ping bez roota, fazy HTTP, DNS) | przed etapem 1 | agent |
| 3 | UI/UX | przed etapem 1, najpóźniej przed 5 | agent (brief: `uptime-monitor-brief-ui-ux.md`) |
| 4 | Dostawcy w UE (VPS, email, magazyn, watchdog) | przed etapem 2 | agent |
| 5 | Kanały push i treść powiadomień | przed etapem 2 | agent |
| 6 | Semantyka heartbeatów i cronów | przed etapem 3 | agent |
| 7 | Format publicznego JSON status page | przed etapem 5 | agent |
| 8 | Kanał agenta przez firewalle i proxy | przed etapem 6 | agent |
| 9 | Format pliku konfiguracyjnego i importy | przed etapem 7 | agent |
| 10 | Model zagrożeń | przed etapem 8 | agent, potem przegląd człowieka |
| 11 | Nazwa projektu i znak towarowy | przed etapem 8 | agent |
| 12 | Prawo: regulamin, prywatność, DPA, kanały spoza UE | w trakcie etapu 8 | prawnik (agent może przygotować pytania) |

---

## 1. Stack Java i dwie bazy (przed etapem 0)
- Framework backendu dla jednego buildu z rolami włączanymi konfiguracją: porównanie popularnych frameworków pod kątem prostoty, startu, pamięci na małym VPS, długoterminowego wsparcia.
- Wątki wirtualne (albo inny model) dla executora wykonującego tysiące checków.
- Narzędzie migracji działające identycznie na PostgreSQL i HSQLDB. Co z typami dat, JSON, UUID, upsertem i blokadami nie działa na obu bazach.
- Blokada lidera jako zwykła tabela: wzorce, czas ważności, zachowanie przy utracie połączenia.
- Testy na obu bazach w CI (np. kontenery testowe).
- Frontend: biblioteka komponentów z dostępnością, trybem ciemnym i i18n, generowanie formularzy z opisu (schematu), wykresy dla 5 000 checków.
- Wynik: rekomendacja z uzasadnieniem i listą ryzyk.

## 2. Pomiary sieciowe w Javie (przed etapem 1)
- **Ping (ICMP) bez uprawnień administratora.** Plan zakłada agenta bez konta administratora, a ICMP zwykle wymaga specjalnych uprawnień. Do sprawdzenia: niezależne od uprawnień gniazda ICMP w Linuksie, uprawnienia kontenera, zachowanie na Windows i macOS, zapasowa ścieżka przez TCP.
- **Czasy w fazach** (DNS, połączenie, TLS, pierwszy bajt): czy standardowy klient HTTP w Javie je udostępnia, czy potrzebny jest niższy poziom.
- **Własne rozwiązywanie DNS z przypięciem adresu** (ochrona przed DNS rebinding w bramie ruchu wychodzącego), IPv6, zachowanie przy wielu adresach.
- Certyfikaty: pełny łańcuch, własne CA, wykrywanie braku odnowienia.
- Kategoria błędu: które wyjątki sieciowe oznaczają „cel nie odpowiada”, a które „lokalizacja nie może wyjść do sieci”.

## 3. UI/UX (przed etapem 1, najpóźniej przed 5)
Osobny brief: `uptime-monitor-brief-ui-ux.md`.

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
- Agent (przejęty serwer, przejęty agent), SSRF i brama ruchu wychodzącego, weryfikacja celów, izolacja przestrzeni, link w emailu, zgłoszenia nadużyć.
- Lista kontrolna według uznanego standardu weryfikacji bezpieczeństwa aplikacji.
- Wynik: lista zagrożeń z obecnym zabezpieczeniem i lukami.

## 11. Nazwa projektu i znak towarowy (przed etapem 8)
- Wolna nazwa: GitHub, rejestry pakietów, domena, kolizje z istniejącymi narzędziami monitoringu.
- Wstępne sprawdzenie w bazach znaków towarowych UE.

## 12. Prawo (w trakcie etapu 8, przed etapem 9)
- Regulamin, polityka prywatności, umowa powierzenia (DPA).
- Kanały powiadomień spoza UE (Pushover, Telegram, Slack): czy i na jakich warunkach.
- Adresy subskrybentów i odbiorców alertów jako dane osobowe.
- Później: VAT w UE i faktury przy płatnościach.
- Agent może przygotować listę pytań do prawnika, ale decyzje podejmuje prawnik.
