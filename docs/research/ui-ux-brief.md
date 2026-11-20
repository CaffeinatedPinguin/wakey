# Brief researchu UI/UX: uptime monitor (v1)

Dokument dla osobnego agenta, który robi research UI/UX. Jest samowystarczalny: nie trzeba znać innych dokumentów, choć szczegóły są w `docs/specs/product-features.md` (v0.9).

**Wynik researchu:** `docs/specs/ui-ux.md`.

---

## 1. Cel researchu
Zaprojektować **super klarowny UI** dla aplikacji do monitorowania dostępności (uptime, crony, certyfikaty, agenci w sieciach prywatnych) ze status page, incydentami i planned work.

Oczekiwany wynik to rekomendacje, a nie kod:
- struktura informacji i nawigacja;
- opis ekranów i kluczowych przepływów;
- lista wspólnych komponentów;
- zasady języka i stanów;
- odpowiedzi na otwarte pytania z sekcji 8.

Jeśli dobry UX wymaga zmiany funkcji albo zasady z sekcji 3, zapisz to jako **propozycję zmiany z uzasadnieniem**. Nie zakładaj, że zmiana już zapadła.

---

## 2. Produkt w skrócie
- Open source (AGPL), self-host oraz publiczna chmura z darmowymi kontami, dane tylko w UE.
- Podobne narzędzia: Uptime Kuma, Gatus, Healthchecks.io, OpenStatus. W tle także UptimeRobot, Better Stack, Oh Dear, Instatus, Atlassian Statuspage.
- **Jeden frontend** (React + Vite) zawiera panel i publiczną status page. Wspólne komponenty i jeden język stanów.
- Utrzymuje to jedna osoba, więc UI ma być prosty w utrzymaniu: mało wyjątkowych ekranów, dużo wspólnych komponentów, formularze generowane z opisu pluginu.

### Użytkownicy
| Kto | Czego potrzebuje |
|---|---|
| **Homelab / solo admin** | Szybki start, cisza, gdy wszystko działa, głośny alert na telefon, gdy coś pada |
| **Mały zespół** | Role, wspólne potwierdzanie alertów, incydenty i status page dla klientów |
| **Odwiedzający status page** | W 3 sekundy wiedzieć, czy usługa działa, od kiedy nie działa i co z tym robią. Często na telefonie |
| **Administrator instancji** (self-host albo operator chmury) | Stan samego systemu, plany i limity, nadużycia, z telefonu w podróży |

---

## 3. Ograniczenia, których research nie zmienia
- **PL i EN od pierwszej wersji, tryb ciemny, dostępność** (cel: WCAG 2.2 AA).
- **Najważniejsze akcje działają z telefonu:** potwierdzenie alertu, masowe wyciszenie, pauza, stan systemu.
- **Skala:** UI ma być szybki przy 5 000 checków. Wszystkie listy są stronicowane i filtrowane po stronie serwera.
- **Na żywo:** zmiany stanu pojawiają się bez odświeżania.
- **Stan da się podlinkować:** filtry i zakres czasu są w URL.
- **Bez własnego HTML, CSS i skryptów od użytkowników.** Status page dostosowuje się tylko logiem i kolorami. Incydenty i post-mortemy to tekst z bezpiecznym formatowaniem (nagłówki, listy, linki, kod).
- **Każda wartość konfiguracyjna pokazuje swoje źródło:** domyślna instancji, planu, przestrzeni albo własna (np. „300 s, domyślne instancji”).
- **UI i plik konfiguracyjny są równorzędne.** UI pokazuje, gdy konfiguracja różni się od pliku.
- **Pięć głównych sekcji:** Przegląd, Checki, Incydenty, Status pages, Ustawienia. Do tego osobna Administracja instancji dla właściciela.
- Link „Kod źródłowy” w stopce (wymóg AGPL).

---

## 4. Słownik stanów (wspólny dla panelu, alertów i status page)
| Stan | Znaczenie |
|---|---|
| `UP` | Działa |
| `DEGRADED` | Działa wolno (przekroczony próg czasu odpowiedzi) albo błąd widzi tylko część lokalizacji |
| `DOWN` | Nie działa, potwierdzone ponowieniem i kworum lokalizacji |
| `UNKNOWN` | Nie wiemy: problem leży po stronie monitoringu (nasza awaria, agent offline, sieć lokalizacji) albo brak świeżych danych |
| `MAINTENANCE` | Trwa planned work. Monitoring działa dalej, ale alerty są wyciszone |

**Oznaczenia dodatkowe** (nie są osobnymi stanami):
- **„niestabilny”**: stan często się zmienia (flapping), więc kolejne zmiany są wyciszone;
- **„do sprawdzenia konfiguracji”**: check nigdy jeszcze nie zadziałał (np. zły adres albo firewall). Nie alarmuje, tylko podpowiada, co sprawdzić;
- **„spauzowany”**, z powodem: ręcznie, przekroczony limit planu, porzucony, brak weryfikacji celu;
- **stan weryfikacji celu** (chmura): zweryfikowany, wygasa, niezweryfikowany, tryb z ograniczeniami.

**Kluczowa zasada:** „nasza awaria jest widoczna jako nasza”. Gdy problem leży po stronie monitoringu, UI mówi to wprost na banerze, zamiast pokazywać czerwone checki.

---

## 5. Inwentarz funkcji według ekranów

Oznaczenia: **MVP** = pierwsza wersja, **v1** = kolejna. Projektuj MVP, ale zostaw miejsce na v1.

### 5.1 Przegląd (dashboard)
- Na górze to, co jest zepsute: `DOWN`, potem `DEGRADED`, potem `UNKNOWN`, z czasem trwania i oznaczeniem „niestabilny”. **MVP**
- Baner naszej awarii („problem z wejściem heartbeatów / lokalizacją / agentem / bazą, statusy wstrzymane”). **MVP**
- Niepotwierdzone alerty z przyciskiem potwierdzenia. **MVP**
- Masowe wyciszenie (wszystko albo tag, grupa, zaznaczenie) na określony czas. **MVP**
- Liczniki stanów, które po kliknięciu filtrują listę. **MVP**
- Feed zdarzeń na żywo. **MVP**
- Zdrowie lokalizacji, wejścia heartbeatów i agentów (opóźnienie wykonania, stan łączenia: połączony / ponawia / uśpiony). **MVP**
- Checki „do sprawdzenia konfiguracji”, cele z wygasającą weryfikacją. **MVP**
- Martwe kanały powiadomień i niedostarczone powiadomienia. **MVP**
- Trwające i nadchodzące planned work. **MVP**
- Zużycie planu, gdy blisko limitu. **MVP**
- Baner „konfiguracja różni się od pliku”. **MVP**
- Baner wspólnej przyczyny (wiele checków za tym samym dostawcą pada naraz). **v1**

### 5.2 Checki: lista
- Kolumny: status z oznaczeniami, nazwa, typ, stan weryfikacji celu, uptime, sparkline czasu odpowiedzi, tagi. **MVP**
- Filtry (stan, typ, tag, grupa, lokalizacja), wyszukiwanie, sortowanie. **MVP**
- Akcje masowe: pauza, wznowienie, wyciszenie, edycja interwału, tagów, polityki i progu, usunięcie. **MVP**
- Grupy. **MVP** Zagnieżdżone grupy. **v1**

### 5.3 Checki: dodawanie i edycja
- Minimum: adres i „Zapisz”. Typ wykrywany albo wybierany, reszta zwinięta z domyślnymi wartościami i ich źródłem. **MVP**
- „Testuj” przed zapisem: jednorazowe wykonanie z pełnym wynikiem. **MVP**
- Typy: HTTP(S) z asercjami (kod, słowo kluczowe, ścieżka JSON, nagłówek), TCP, ping, DNS, certyfikat, heartbeat (cron albo okres, grace, start / sukces / błąd, kod wyjścia, wiek ostatniego sukcesu). **MVP** Domain expiry. **v1**
- Pola zaawansowane: interwał, timeout, ponowienia, lokalizacje i próg kworum, próg wolnej odpowiedzi, flapping, polityka alertu, opis z linkiem do runbooka. **MVP**
- **Weryfikacja celu w trakcie dodawania** (chmura): formularz prowadzi przez weryfikację i podpowiada metodę (sekcja 5.4). **MVP**
- Dla heartbeatu: gotowe snippety (curl, skrypt) z ograniczonymi ponowieniami. **MVP**
- Formularze są generowane z opisu typu checka, więc projekt musi być wzorcem, a nie ręcznym ekranem dla każdego typu.

### 5.4 Weryfikacja własności celu (chmura)
- Domena: rekord DNS TXT albo plik pod `/.well-known/`. Obejmuje subdomeny. **MVP**
- Adres IP: albo „wywołaj ten adres z tej maszyny” (gotowe `curl`, osobno IPv4 i IPv6), albo „wystaw token pod tym portem i ścieżką”. **MVP**
- Email: link potwierdzający. **MVP**
- Instrukcja krok po kroku z elementami do skopiowania i przyciskiem „Sprawdź teraz”. Jasny komunikat, dlaczego się nie udało. **MVP**
- Ponowna weryfikacja co 30 dni: ostrzeżenie, okres łaski, pauza. Dla wywołania z IP także stały adres do wpisania w cron. **MVP**
- Tryb „niezweryfikowane z ograniczeniami” (jeśli administrator go odblokował): widoczne ograniczenia (dłuższy interwał, limit liczby). **MVP**

### 5.5 Szczegóły checka
- Aktualny stan, z ilu lokalizacji pochodzi decyzja, powód `UNKNOWN`. **MVP**
- Wykres czasu odpowiedzi w fazach (DNS, połączenie, TLS, pierwszy bajt) per lokalizacja, z linią progu. **MVP**
- Zakresy: 24 h, 7, 30, 60, 365 dni (w granicach planu). **MVP**
- Uptime w okresie, bez fałszywych alarmów. **MVP**
- Zdarzenia i błędy z kategorią („cel nie odpowiada” albo „lokalizacja nie może wyjść do sieci”), pełny request i odpowiedź. **MVP**
- Podpowiedzi dla „do sprawdzenia konfiguracji”. **MVP**
- Certyfikat: wystawca, łańcuch, daty. **MVP**
- Historia powiadomień (kanał, próby, stan końcowy) i historia zmian konfiguracji (z UI, API czy pliku). **MVP**
- Heartbeat: odrzucone pingi (limit) i pingi zapisane z bufora po naszej awarii. **MVP**
- „Sprawdź teraz”, pauza, wyciszenie, klonowanie. **MVP**

### 5.6 Agenci (w Ustawieniach → Lokalizacje i agenci)
- Dodanie agenta: jednorazowy token i gotowe polecenie instalacji. **MVP**
- Diagnostyka: wersja, ostatni kontakt, ostatni błąd, stan łączenia, bufor, cele odrzucone przez lokalną listę agenta, odsetek błędów sieci. **MVP**
- Alerty „agent offline” i „agent prawdopodobnie ma problem z siecią” (jeden alert zamiast fali `DOWN`). **MVP**
- „Dostępna nowa wersja” (aktualizację robi użytkownik). **MVP**
- Ważne dla UX: agent ma lokalną listę dozwolonych celów, której serwer nie może zmienić. Check spoza listy jest odrzucany, a UI musi to jasno wyjaśnić.

### 5.7 Alerty i powiadomienia
- Kanały: email, webhook z szablonem, Pushover. „Wyślij test”, potwierdzenie odbiorcy, stan zdrowia kanału (martwy). **MVP** ntfy, Telegram, Slack, Discord, Teams. **v1**
- Polityki: zdarzenia, opóźnienie przed wysłaniem, powtarzanie do potwierdzenia z limitem, kanał zapasowy. **MVP** Ciche godziny, eskalacje. **v1**
- Potwierdzenie (ack) z linku w powiadomieniu i w UI, snooze, masowe wyciszenie. **MVP**
- Alert zbiorczy: kilka awarii w krótkim oknie to jedno powiadomienie z listą. **MVP**
- Log dostarczeń: kanał, próby, odpowiedź odbiorcy, zapas, stan końcowy („nie dostarczono”). **MVP**
- Limit powiadomień w czasie z planu: komunikat „limit osiągnięty”. **MVP**
- **Treść samego powiadomienia** (email, push, webhook) też jest częścią UX: co jest w tytule, link do ack i do szczegółów, czytelność na zablokowanym ekranie telefonu.

### 5.8 Incydenty i planned work
- Lista incydentów, statusy (badanie → zidentyfikowano → monitorowanie → rozwiązano), dotknięte komponenty, wpływ. **MVP**
- **Oś z hierarchią:** aktualizacje ludzi jako wyraźne karty, zdarzenia systemowe zwinięte do jednej linii i zapisane zdaniami, najnowsze na górze, pole nowej aktualizacji na górze. Ten sam komponent dla incydentów i planned work. **MVP**
- Incydenty automatyczne przy `DOWN` (publikowane od razu albo jako szkic do zatwierdzenia). **MVP**
- Post-mortem jako tekst z formatowaniem. **MVP**
- Oznaczanie fałszywych alarmów, pojedynczo i masowo dla okresu. **MVP**
- Planned work: jednorazowe i cykliczne okna, strefa czasowa, komponenty, flaga „wlicz do SLA”. **MVP**
- Szablony aktualizacji, powiadomienia subskrybentów. **v1**

### 5.9 Status pages (zarządzanie w panelu)
- Komponenty i grupy powiązane z checkami, ręczne nadpisanie stanu. **MVP**
- Logo i kolory (z kontrolą kontrastu), podgląd, który jest tą samą stroną co publiczna. **MVP**
- Stan zawieszenia (chmura) z powodem. **MVP**
- Własna domena, prywatna strona, subskrypcje, badge'e, RSS. **v1**

### 5.10 Publiczna status page
- Ogólny stan, komponenty i grupy, pasek historii (domyślnie 90 dni), aktualne i przeszłe incydenty, planned work. **MVP**
- Wiek danych i czas ostatniej aktualizacji. Strona działa z ostatniej kopii przy awarii serwera i wtedy pokazuje, jak stare są dane. **MVP**
- Tryb ciemny, PL/EN, telefon jako główny ekran. **MVP**
- „Zgłoś nadużycie” w stopce (chmura). **MVP**
- Subskrypcje email i webhook z filtrem komponentów, RSS/Atom. **v1**

### 5.11 Ustawienia przestrzeni
Domyślne wartości (ze źródłem), zweryfikowane cele, tryb incydentów automatycznych, okno alertu zbiorczego, kanały i polityki, lokalizacje i agenci, użytkownicy i role (Admin, Edytor, Podgląd), zaproszenia, logowanie (chmura: własne OIDC przestrzeni), tokeny API, sekrety (zamaskowane, tylko podmiana), plan i zużycie limitów, konfiguracja (eksport do pliku, lista różnic), dziennik audytu, eksport i usunięcie przestrzeni. **MVP**

### 5.12 Administracja instancji
Przestrzenie i plany, rejestracja (zaproszenia, lista oczekujących), sposoby logowania, tryb weryfikacji celów, zgłoszenia nadużyć i zawieszanie stron, lokalizacje i cele kontrolne, limity na wejściu, polityka ponowień, polityka ruchu wychodzącego, porzucone checki, klucz szyfrowania (rotacja, stan kopii), zrzuty bazy, **stan systemu** (wersja, lider, opóźnienie wykonania, kontrola wejścia, obciążenie, liczba alertów względem normy, elementy „uśpione”, zaległości archiwum, watchdog, ostatni zrzut i test odtworzenia). Stan systemu musi być czytelny na telefonie. **MVP**

### 5.13 Logowanie i pierwsze uruchomienie
- Self-host: kreator pierwszego uruchomienia (lokalny admin, 2FA), potem OIDC. **MVP**
- Chmura: GitHub, Google, link w emailu, rejestracja z kodem zaproszenia albo lista oczekujących, jasna informacja „jedna lokalizacja, bez SLA”. **MVP**
- Puste stany prowadzą: pierwszy check, pierwsza weryfikacja, pierwszy agent, pierwszy heartbeat, pierwsza status page. **MVP**

---

## 6. Kluczowe przepływy do zaprojektowania
1. **Od zera do pierwszego alertu na telefonie** (self-host i chmura): ile kroków, gdzie użytkownik może utknąć.
2. **Dodanie checka na cel wymagający weryfikacji** (domena, samo IP z pingiem, IP z HTTP).
3. **Instalacja agenta za firewallem**, który na początku nie działa: od „do sprawdzenia konfiguracji” do pierwszego `UP`.
4. **Alert w nocy:** powiadomienie → ack z telefonu → szczegóły → incydent → aktualizacja na status page.
5. **Globalna awaria dużego dostawcy:** 200 checków pada naraz → alert zbiorczy → masowe wyciszenie → oznaczenie fałszywych alarmów po fakcie.
6. **Nasza awaria:** wejście heartbeatów albo agent nie działa → baner, `UNKNOWN`, jeden alert, a nie czerwona lista.
7. **Planned work:** zaplanowanie okna → widoczność na status page → automatyczny start i koniec.
8. **Konfiguracja w gicie:** zmiana w UI → baner różnicy → eksport do pliku → wdrożenie z konfliktem.
9. **Przekroczenie limitu planu:** ostrzeżenie → okres na reakcję → pauza wybranych checków → przywrócenie.
10. **Odwiedzający status page w trakcie awarii**, na telefonie.
11. **Administrator instancji w podróży:** sprawdza stan systemu i zawiesza zgłoszoną stronę z telefonu.

---

## 7. Co zbadać u innych
- **Uptime Kuma:** co ludzie lubią (szybki start, wygląd) i na co narzekają (wolny przy setkach monitorów, brak ról, brak API).
- **Healthchecks.io:** prostota, cisza, UX cronów i heartbeatów, treść maili.
- **Gatus:** konfiguracja w pliku i jak UI to pokazuje.
- **OpenStatus:** status page, oś incydentu (ich własna krytyka osi „jak plik logów”), prywatne lokalizacje.
- **Better Stack, UptimeRobot, Oh Dear, Instatus, Atlassian Statuspage:** status page, incydenty, alerty na telefon, masowe wyciszanie.
- **Wzorce z narzędzi on-call** (np. PagerDuty, Grafana OnCall): potwierdzanie z telefonu, alert zbiorczy.
- Źródła opinii: issues na GitHubie, wątki na Hacker News i Reddit (r/selfhosted, r/homelab), recenzje.

---

## 8. Otwarte pytania, na które research ma odpowiedzieć
1. **Co widzi odwiedzający status page, gdy komponent jest `UNKNOWN` z naszej winy?** Ostatni znany stan z wiekiem danych? Neutralny baner? Jak nie skłamać ani zielonym, ani czerwonym kolorem?
2. Jak pokazać **decyzję z wielu lokalizacji** (kworum) tak, żeby była zrozumiała bez znajomości słowa „kworum”?
3. Jak odróżnić wizualnie `DEGRADED` „wolno” od `DEGRADED` „część lokalizacji widzi błąd”? Czy potrzebne są dwa warianty?
4. Jak pokazać **źródło wartości konfiguracyjnej** bez zaśmiecania formularzy?
5. Jak pokazać **różnicę między UI a plikiem** na liście i w szczegółach obiektu?
6. Jak oddzielić oznaczenia („niestabilny”, „do sprawdzenia konfiguracji”, „spauzowany”, „w trybie z ograniczeniami”) od stanów, żeby lista przy 5 000 checków była czytelna?
7. Kolory stanów: paleta czytelna dla daltonistów, w trybie jasnym i ciemnym, z ikoną albo kształtem obok koloru.
8. Ile informacji zmieści się w powiadomieniu push, żeby dało się podjąć decyzję bez otwierania aplikacji?
9. Nawigacja na telefonie: które ekrany muszą być pełne, a które mogą być tylko do podglądu?

---

## 9. Oczekiwane wyniki
1. **Mapa informacji i nawigacja** (desktop i telefon).
2. **Opis każdego ekranu z sekcji 5:** co jest na górze, co zwinięte, puste stany, stany błędu i ładowania.
3. **Przepływy z sekcji 6** krok po kroku, z miejscami, gdzie użytkownik może utknąć.
4. **Lista wspólnych komponentów** (np. odznaka stanu, oś zdarzeń, baner, pasek historii, wykres w fazach, wartość ze źródłem, lista z akcjami masowymi, kreator weryfikacji), żeby frontend miał ich jak najmniej.
5. **Tokeny wyglądu:** kolory stanów (jasny i ciemny), typografia, odstępy, ikony stanów.
6. **Zasady treści PL/EN:** ton, nazwy stanów, wzory komunikatów błędów i podpowiedzi.
7. **Odpowiedzi na pytania z sekcji 8**, z uzasadnieniem i przykładami z innych narzędzi.
8. **Lista propozycji zmian funkcji**, jeśli research je wskaże (osobno, z uzasadnieniem).

## 10. Poza zakresem
Wybór biblioteki komponentów i technologii frontendu (osobny pkt 1.5 w `docs/research/backlog.md`), kod, branding i nazwa produktu, ceny planów.
