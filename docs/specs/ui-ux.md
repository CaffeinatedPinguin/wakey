# Specyfikacja UI/UX: uptime monitor (v1.1)

**Wersja:** 1.1 — połączenie `docs/research/ui-ux-brief.md`, specyfikacji UI/UX v1.0 i raportu „Research UX/UI Monitora Dostępności” (PDF)  
**Stan:** 19 listopada 2026 r.  
**Zakres:** architektura informacji, nawigacja, ekrany, przepływy, komponenty, tokeny wizualne, język, powiadomienia, decyzje UX dla MVP z miejscem na v1.

> **Główna zasada produktu:** użytkownik ma w kilka sekund zrozumieć, co nie działa, dlaczego system tak uważa, czy dane są aktualne i co powinien teraz zrobić.

To specyfikacja zachowania, nie projekt graficzny ani specyfikacja backendu. Przed implementacją wizualną trzeba przygotować makiety kluczowych ekranów i podjąć decyzje z sekcji 12.

---

## Contents

- [0. Jak czytać ten dokument](#0-jak-czytać-ten-dokument)
- [1. Najważniejsze decyzje UX](#1-najważniejsze-decyzje-ux)
- [2. Model stanów i oznaczeń](#2-model-stanów-i-oznaczeń)
- [3. Architektura informacji i nawigacja](#3-architektura-informacji-i-nawigacja)
- [4. Specyfikacja ekranów](#4-specyfikacja-ekranów)
- [5. Kluczowe przepływy](#5-kluczowe-przepływy)
- [6. Wspólne komponenty](#6-wspólne-komponenty)
- [7. Konfiguracja i GitOps w UI](#7-konfiguracja-i-gitops-w-ui)
- [8. Tokeny wizualne i dostępność](#8-tokeny-wizualne-i-dostępność)
- [9. Język PL/EN i powiadomienia](#9-język-plen-i-powiadomienia)
- [10. Research konkurencji — wnioski](#10-research-konkurencji--wnioski)
- [11. Odpowiedzi na pytania z briefu (sekcja 8)](#11-odpowiedzi-na-pytania-z-briefu-sekcja-8)
- [12. Propozycje zmian i decyzje do podjęcia](#12-propozycje-zmian-i-decyzje-do-podjęcia)
- [13. Priorytety projektowania](#13-priorytety-projektowania)
- [14. Skrót decyzji dla implementacji](#14-skrót-decyzji-dla-implementacji)
- [15. Rejestr zmian v1.0 → v1.1](#15-rejestr-zmian-v10--v11)
- [16. Źródła](#16-źródła)
- [17. Końcowa rekomendacja](#17-końcowa-rekomendacja)


# 0. Jak czytać ten dokument

Trzy rodzaje zapisów:

- **Wymaganie briefu** — ograniczenie lub funkcja z briefu. Nie zmieniać po cichu.
- **Rekomendacja UX** — domyślny kierunek implementacji.
- **Propozycja zmiany** (oznaczona **[PZ]**) — wpływa na zakres, model domenowy lub API. Wymaga świadomej decyzji, nie jest automatycznie w MVP.

Przy sprzeczności wymaganie briefu ma pierwszeństwo przed rekomendacją UX.

## 0.1 Kontekst produktu

- Open source (AGPL); self-host oraz publiczna chmura z darmowymi kontami; dane chmurowe tylko w UE.
- Jeden frontend React + Vite: panel monitoringu/administracji i publiczna status page.
- Monitoring: uptime (HTTP(S), TCP, ping, DNS), certyfikaty, crony/heartbeaty, usługi w sieciach prywatnych przez agentów.
- Status pages, incydenty, planned work, alerty, przestrzenie, administracja instancji.
- Utrzymuje jedna osoba: mało wyjątkowych ekranów, wspólne komponenty, formularze generowane z definicji pluginu, mało zależności.

## 0.2 Użytkownicy

| Użytkownik | Najważniejsza potrzeba |
|---|---|
| Homelab / solo admin | Szybki start, cisza, gdy wszystko działa, głośny alert na telefon, gdy coś pada |
| Mały zespół | Role, wspólne potwierdzanie alertów, incydenty i status page dla klientów |
| Odwiedzający status page | W ~3 sekundy: czy działa, od kiedy nie działa, co zespół robi. Najczęściej na telefonie |
| Administrator instancji | Stan systemu, plany i limity, nadużycia — także z telefonu w podróży |

## 0.3 Niezmienne ograniczenia (z briefu)

- PL i EN od pierwszej wersji; tryb jasny i ciemny; WCAG 2.2 AA.
- Z telefonu: ACK, masowe wyciszenie, pauza/wznowienie, aktualizacja incydentu, stan systemu.
- Skala 5 000 checków: wszystkie listy stronicowane i filtrowane po stronie serwera.
- Zmiany stanu na żywo, bez odświeżania.
- Filtry, wyszukiwanie i zakres czasu w URL.
- Bez własnego HTML/CSS/JS od użytkowników. Status page: tylko logo i kolory. Incydenty i post-mortemy: bezpieczne formatowanie (nagłówki, listy, linki, kod).
- Każda wartość konfiguracyjna pokazuje źródło: instancja, plan, przestrzeń, własna.
- UI i plik konfiguracyjny są równorzędne; UI pokazuje różnice.
- Pięć sekcji panelu: Przegląd, Checki, Incydenty, Status pages, Ustawienia + osobna Administracja instancji.
- Link „Kod źródłowy” w stopce (AGPL).

---

# 1. Najważniejsze decyzje UX

1. **Interfejs operacyjny, nie galeria wykresów.** Przegląd odpowiada na pytania: co jest zepsute, czy to usługa czy monitoring, jak długo, czy ktoś potwierdził, co mogę zrobić teraz. Zdrowe checki są w licznikach i na liście, nie na Przeglądzie.
2. **Awaria monitoringu nie wygląda jak awaria usług.** Niewiarygodne dane → `UNKNOWN`, baner „Problem z monitoringiem”, jeden alert systemowy, przyczyna i wiek ostatnich wiarygodnych danych. Zamiast czerwonej ściany chłodny, łupkowy kolor UNKNOWN.
3. **Stan, powód, oznaczenia i metadane to cztery różne rzeczy.** Nie łączyć ich w jeden kolorowy badge.
4. **ACK ≠ rozwiązanie.** ACK to przejęcie odpowiedzialności. Rozwiązanie incydentu to osobna akcja.
5. **Jeden wzorzec, wiele zastosowań.** Wspólne komponenty (sekcja 6) zamiast osobnych ekranów dla typów checków.
6. **Żadna szeroka akcja bez podglądu zakresu.** Masowe wyciszenie zawsze z liczbą objętych checków i z czasem wygaśnięcia (bez opcji „na zawsze”).

---

# 2. Model stanów i oznaczeń

## 2.1 Pięć stanów bazowych

| Kod | Znaczenie | Etykieta PL (badge) | Etykieta EN (badge) | Opis na status page (PL / EN) |
|---|---|---|---|---|
| `UP` | Usługa działa | Działa | Operational | Działa prawidłowo / Operational |
| `DEGRADED` | Wolno albo problem widzi część lokalizacji | Działa z problemami | Degraded | Usługa odpowiada wolniej lub część żądań zawodzi / Experiencing slow responses or partial errors |
| `DOWN` | Awaria potwierdzona ponowieniem i wymaganą liczbą lokalizacji | Nie działa | Down | Usługa jest niedostępna / Service unavailable |
| `UNKNOWN` | Brak świeżych/wiarygodnych danych albo problem po stronie monitoringu | Stan nieznany | Unknown | Nie możemy teraz potwierdzić stanu (problem z pomiarem) / We can't confirm the current status (measurement issue) |
| `MAINTENANCE` | Trwa planned work; monitoring działa, alerty wyciszone | Prace planowe | Maintenance | Trwają zaplanowane prace / Scheduled maintenance in progress |

Rozstrzygnięcia nazewnicze:

- **Krótka etykieta w badge’u, pełne zdanie w opisie.** „Nie można potwierdzić stanu” (v1.0) jest za długie na badge; zostaje jako opis, a badge to „Stan nieznany”.
- **Nie „Obniżona wydajność”** (propozycja PDF) — nie pasuje do DEGRADED z powodu części lokalizacji.
- **Nie „Awaria” jako etykieta DOWN** — koliduje z „awarią monitoringu”. W UI „awaria” zawsze z dopełnieniem („awaria monitoringu”, „awaria usługi”).
- Kod stanu (`DOWN`) jest widoczny w szczegółach technicznych i w tytułach powiadomień (sekcja 9.5); w pozostałym UI dominuje etykieta dla człowieka.

Nie dodawać kolejnych stanów bazowych dla odmian problemów.

## 2.2 Powód DEGRADED

Jeden stan, dwa powody, rozróżnione podetykietą i małą ikoną (bez nowego koloru):

- **Wolna odpowiedź** — ikona zegara: `Wolno (640 ms > 500 ms)`.
- **Część lokalizacji** — ikona lokalizacji: `Błąd w 1/3 lok.`, nazwa lokalizacji w drugim wierszu (mobile) lub w szczegółach.

**[PZ] API:** backend zwraca stabilne pole `degradedReason` (`slow` | `partial_locations`), frontend niczego nie zgaduje.

## 2.3 UNKNOWN i wiek danych

`UNKNOWN` jest bieżącym stanem także wtedy, gdy znany jest historyczny UP/DOWN. Historyczny wynik pokazujemy wyłącznie jako „ostatni potwierdzony” z czasem — nigdy jako aktualny zielony UP.

Status page:

> **Nie możemy potwierdzić aktualnego stanu**  
> Problem z systemem monitorowania. Nie otrzymujemy aktualnych danych.  
> Ostatni potwierdzony stan: **Działa** · 6 minut temu.

**Ręczny stan ma pierwszeństwo.** Jeżeli zespół opublikował incydent lub ręcznie nadpisał stan komponentu, deklaracja człowieka wygrywa z automatycznym UNKNOWN.

**[PZ] API:** każde UNKNOWN ma jawny powód (`ingress_down`, `agent_offline`, `location_offline`, `stale_data`, `db_unavailable`…).

## 2.4 Decyzja z wielu lokalizacji (bez słowa „kworum”)

- Lista: kompaktowo `2/3 lok.` obok badge’a.
- Szczegóły: „Nie działa — 2 z 3 lokalizacji potwierdzają błąd (wymagane: co najmniej 2)”, „Działa — potwierdzone z 3 z 3 lokalizacji”, „Wymagane 2 lokalizacje; obecnie 1 z 3 widzi problem”.
- Formularz: pole „Ile lokalizacji musi potwierdzić awarię”, podpowiedź „Więcej niż jedna lokalizacja chroni przed fałszywym alarmem z jednej sieci”.
- Lokalizacja bez odpowiedzi = **brak danych**, nigdy UP.

## 2.5 Oznaczenia (nie są stanami)

| Oznaczenie | Znaczenie | Prezentacja |
|---|---|---|
| Niestabilny / Flapping | Stan często się zmienia; kolejne alerty wyciszone | Mikro-odznaka przy nazwie |
| Do sprawdzenia konfiguracji | Check nigdy nie zadziałał (zły adres, firewall, cel spoza allowlisty agenta) | Neutralna ikona koła zębatego w kolumnie stanu + podpowiedź |
| Spauzowany | Monitoring zatrzymany; powód jawny: ręcznie, limit planu, porzucony, brak weryfikacji | Neutralna ikona pauzy w kolumnie stanu + powód pod nazwą |
| Weryfikacja wygasa | Cel wymaga ponownej weryfikacji | Mikro-odznaka ostrzegawcza z datą |
| Niezweryfikowany | Cel nie przeszedł weryfikacji | Mikro-odznaka |
| Tryb z ograniczeniami | Monitorowany z limitami | Mikro-odznaka z opisem limitów |

**Reguła kolumny stanu (rozstrzygnięcie):** kolumna stanu pokazuje jeden z pięciu stanów bazowych. Wyjątek dotyczy checków, które **nie mierzą celu aktywnie** (spauzowany, do sprawdzenia konfiguracji, oczekuje na pierwszy wynik). W ich miejscu stoi neutralna szara ikona (pauza / koło zębate / klepsydra), bo pokazanie DOWN albo UNKNOWN byłoby nieprawdą. To reguła prezentacji, nie szósty stan domenowy. Oznaczenia modyfikujące aktywny pomiar (niestabilny, weryfikacja wygasa, tryb z ograniczeniami) to mikro-odznaki przy nazwie: maks. dwie widoczne, reszta pod „+1”.

**Nowy check przed pierwszym wynikiem** (luka w briefie): pokazywać „Oczekuje na pierwsze sprawdzenie” z odliczaniem. Nie używać UNKNOWN, bo ono oznacza problem z danymi/monitoringiem. Wzorzec: stan „New” z Healthchecks.io.

---

# 3. Architektura informacji i nawigacja

## 3.1 Jeden frontend, dwa shelle

1. **Panel** — autoryzacja, nawigacja, przestrzenie, akcje, ustawienia.
2. **Publiczna status page** — lekki shell bez nawigacji panelu, mobile-first, PL/EN, jasny/ciemny, działa z ostatniej kopii przy awarii API.

Wspólne: język stanów, tokeny, StatusBadge, HistoryBar, renderer SafeRichText. Layout nie musi być wspólny.

## 3.2 Desktop

- Lewy sidebar ~220–240 px: Przegląd, Checki, Incydenty, Status pages, Ustawienia.
- Górny pasek: przełącznik przestrzeni, globalne wyszukiwanie (skrót `/`), szybkie dodanie checka, przełącznik PL/EN i motywu, profil.
- **Administracja instancji** — widoczna tylko dla właściciela, jako osobny przycisk o odmiennym akcencie kolorystycznym, żeby nie pomylić ustawień przestrzeni z konfiguracją serwera.
- Typy checków, lokalizacje i agenci nie są głównymi pozycjami nawigacji.
- Stopka panelu i status page: „Kod źródłowy”.

## 3.3 Telefon (< 768 px)

Dolny pasek, cele dotyku 44–48 px: **Przegląd** (z plakietką liczby niepotwierdzonych alertów), **Checki**, **Incydenty**, **Więcej** (Status pages, Ustawienia, Lokalizacje i agenci, przełączanie przestrzeni, Administracja instancji).

### Macierz funkcji mobilnych

| Funkcja | Telefon |
|---|---|
| Przegląd, banery, ACK / cofnięcie ACK | Pełne |
| Pauza / wznowienie, „Sprawdź teraz” | Pełne |
| Wyciszenie pojedyncze i masowe (tag, grupa, alert zbiorczy, filtr) | Pełne, z licznikiem i potwierdzeniem; zakres „wszystko” patrz [PZ] w 12.2 |
| Szczegóły checka | Pełne, jedna kolumna |
| Lista checków | Karty (stan z ikoną, nazwa, czas trwania, ostatni czas odpowiedzi) + szybkie akcje |
| Incydenty i planned work: kompozytor, zmiana statusu | Pełne; panel właściwości jako zwijany arkusz nad osią |
| Tworzenie planned work | Uproszczone |
| Publiczna status page | Pełna, mobile-first |
| Administracja: Stan systemu, zgłoszenia nadużyć, zawieszenie strony | Pełne, z potwierdzeniem |
| Dodanie prostego checka | Dostępne |
| Ustawienia: test kanału, podmiana sekretu, odświeżenie agenta | Pełne (akcje awaryjne) |
| Edycja masowa w tabeli, asercje JSON, szablony webhooków, konfigurator kolorów, duże konflikty konfiguracji, rotacja kluczy, role/OIDC | Podgląd + komunikat „Wygodniej na szerszym ekranie”; świadome odblokowanie tylko dla operacji awaryjnych |
| Audyt, eksporty | Podgląd / pobranie |

## 3.4 Stan w URL

Każdy filtr, wyszukiwanie, sortowanie, strona, aktywna zakładka i zakres czasu siedzą w URL, więc jednym linkiem przekazuje się dokładny widok.

| Sekcja | Przykładowe parametry |
|---|---|
| Przegląd | `state`, `tag`, `group`, `ack` |
| Checki | `q`, `state`, `type`, `tag`, `group`, `loc`, `verify`, `diff`, `paused_reason`, `sort`, `page` |
| Szczegóły checka | `range`, `loc`, `tab` |
| Incydenty | `kind` (incident / maintenance), `status`, `range`, `page` |
| Status pages | `pageId`, `tab` |
| Ustawienia | `section`, `agentId`, `diffOnly` |
| Administracja | `adminTab`, `workspaceId`, `abuseStatus` |

---

# 4. Specyfikacja ekranów

Wszystkie widoki panelu mają ten sam szkielet: **kontener banerów globalnych** (całkowicie ukryty, gdy nie ma problemów), nagłówek z tytułem i główną akcją, strefa robocza.

**Aktualizacje na żywo (WebSocket/SSE):**
- Przegląd: kolejka problemów i liczniki aktualizują się natychmiast.
- Tabele stronicowane: widoczne wiersze zmieniają stan w miejscu, **bez zmiany kolejności pod kursorem**. Pozycje, które wchodzą do filtra lub z niego wypadają, sygnalizuje subtelny pasek „Lista uległa zmianie — odśwież”.
- Dostępność: zmiany ogłaszane przez `aria-live="polite"` tylko dla istotnych zdarzeń (nowy DOWN/UNKNOWN na Przeglądzie), zdławione, bez czytania każdej zmiany z 5 000 checków. Animacje respektują `prefers-reduced-motion`.

## 4.1 Przegląd (triage)

To nie jest lista wszystkich checków ani pulpit NOC.

### Kolejność

1. **Banery** (tylko gdy dotyczą): problem z monitoringiem → konfiguracja różni się od pliku → limit planu przekroczony/blisko. W v1: baner „możliwa wspólna przyczyna”.
2. **Kolejka problemów:** DOWN → DEGRADED → UNKNOWN wymagające uwagi. Wewnątrz każdej grupy niepotwierdzone na górze. Przy każdym wpisie inline „Potwierdź” i „Wycisz”.
3. **Pasek akcji:** „Wycisz masowo”.
4. **Klikalne liczniki:** DOWN, DEGRADED, UNKNOWN, MAINTENANCE, UP, Spauzowane → lista Checków z filtrem w URL.
5. **Zwijane sekcje** (domyślnie zwinięte, gdy licznik = 0): zdrowie lokalizacji, wejścia heartbeatów i agentów (opóźnienie, połączony/ponawia/uśpiony); do sprawdzenia konfiguracji; wygasające weryfikacje; martwe kanały i niedostarczone powiadomienia; trwające/nadchodzące planned work; zużycie planu.
6. **Feed zdarzeń na żywo** — wąska prawa kolumna (desktop) / dół (telefon). Tylko zmiany stanu i akcje ludzi, powtarzalne zdarzenia grupowane.

> **Rozstrzygnięcie:** v1.0 miało osobne sekcje „Aktywne problemy” i „Niepotwierdzone alerty”. To zwykle te same checki, więc powstawały dwie listy z duplikatami. Teraz jest jedna kolejka z ACK inline i filtrem „tylko niepotwierdzone”.

### Wpis problemu

Badge stanu, nazwa, czas trwania, krótki powód, `2/3 lok.` (jeśli dotyczy), oznaczenia (np. niestabilny), status ACK (kto, kiedy) albo przycisk „Potwierdź”. Surowe logi i payloady tylko w szczegółach lub drawerze.

### Masowe wyciszenie

Dialog: zakres (wszystko, tag, grupa, zaznaczenie, bieżący filtr) + czas trwania (np. 30 min, 1 h, 4 h, do konkretnej godziny). Przed potwierdzeniem pokazuje:
- dokładną liczbę checków i kryterium,
- czas wygaśnięcia (wymagany — **brak opcji „na zawsze”**),
- link „Pokaż objęte checki”,
- dodatkowe potwierdzenie dla szerokich zakresów (np. > 20 checków lub „wszystko”).

Ważne: wyciszenie dotyczy checków, nie alertów. Nowy DOWN na wyciszonym checku nie wysyła powiadomienia, ale **pokazuje się w kolejce** z oznaczeniem „wyciszony”, żeby nie przeoczyć niezależnej awarii.

### Stany

| Sytuacja | Zachowanie |
|---|---|
| Brak checków | Karty startowe: pierwszy check HTTP, heartbeat, agent, status page |
| Wszystko działa | „Wszystkie monitorowane usługi działają” + kompaktowe liczniki; bez wykresów |
| Awaria monitoringu | Neutralny baner, łupkowe UNKNOWN, nie czerwona lista |
| Ładowanie | Szkielety 6 liczników i kilku wierszy; nigdy zera jako wartości |
| Utracone połączenie live | „Utracono połączenie na żywo — dane mogą być nieaktualne” + czas ostatniej aktualizacji + „Połącz ponownie” |
| Błąd API | Przyczyna, wiek cache, „Ponów” |

## 4.2 Checki — lista

Od początku dla 5 000+ checków: paginacja, filtrowanie, sortowanie i agregacja po stronie serwera. Bez ładowania historii do klienta (główna przyczyna problemów Uptime Kuma).

| Kolumna | Zawartość |
|---|---|
| Stan | StatusBadge (lub neutralna ikona wg 2.5) + krótki powód + `2/3 lok.` |
| Nazwa | Nazwa + typ (mała etykieta), mikro-odznaki, ikona różnicy z plikiem |
| Weryfikacja | Tylko w chmurze / gdy funkcja włączona |
| Uptime | Dla wybranego okresu, `tabular-nums` |
| Odpowiedź | Lekki sparkline z **pre-agregowanej** serii (np. 24 punkty z API) |
| Tagi | Etykiety |
| Akcje | Menu kontekstowe |

Bez pełnego URL jako domyślnej kolumny; bez ciężkich wykresów w wierszach.

**Filtry (serwerowe, w URL):** stan, typ, tag, grupa (miejsce na zagnieżdżone w v1), lokalizacja, weryfikacja, „Tylko różniące się od pliku”, powód pauzy, wyszukiwanie po nazwie i celu, sortowanie.

**Akcje masowe** (przypięty pasek po zaznaczeniu): pauza, wznowienie, wyciszenie, interwał, tagi, polityka alertów, próg, usunięcie. Zaznaczenie rozróżnia stronę od całego wyniku: „Zaznaczono 50 widocznych. Zaznacz wszystkie 248 wyników?”. Operacje destrukcyjne i szerokie wymagają potwierdzenia z liczbą.

**Stany:** ładowanie — nagłówki + szkielet 10 wierszy; brak checków — „Dodaj pierwszy check”; brak wyników — „Brak checków spełniających kryteria” + „Wyczyść filtry”; błąd — zachowane parametry URL + „Ponów”.

## 4.3 Dodawanie i edycja checka

**Progressive disclosure.** Na górze tylko: cel/adres (typ rozpoznawany przy wklejeniu, z możliwością zmiany), nazwa (podpowiadana z celu), **Testuj**, **Zapisz**.

Typy MVP: HTTP(S) z asercjami (kod, słowo kluczowe, ścieżka JSON, nagłówek), TCP, ping, DNS, certyfikat, heartbeat. v1: domain expiry.

**Ustawienia zaawansowane** (akordeony generowane z definicji typu): interwał, timeout, ponowienia, lokalizacje i liczba potwierdzeń, próg wolnej odpowiedzi, flapping, polityka alertu, opis i link do runbooka, asercje.

**Źródło wartości (bez zaśmiecania):**
- wartość dziedziczona: stonowana + dyskretny podpis `Domyślne przestrzeni (300 s)`;
- po zmianie: `Własne · nadpisuje 300 s z przestrzeni · Przywróć`;
- nagłówek akordeonu: `Zaawansowane (2 własne, reszta domyślna)`.

**Testuj:** jednorazowe wykonanie, wynik w zwijanym panelu nad formularzem: wynik i czas, lokalizacja, kod/odpowiedź zależna od typu, fazy DNS/TCP/TLS/TTFB, spełnienie asercji, kategoria błędu („cel nie odpowiada”, „lokalizacja nie może wyjść do sieci”, „błąd autoryzacji”). Pełny request/response po rozwinięciu.

**Reguły formularza:** wartości domyślne wypełnione; zmiana typu nie kasuje danych bez ostrzeżenia; walidacja inline z działaniem naprawczym; formularz zachowuje postęp (weryfikacja, dodanie kanału, chwilowe zamknięcie).

**Brak kanału powiadomień:** jeśli przestrzeń nie ma aktywnego kanału, pod polem polityki pojawia się „Dodaj kanał” otwierający panel boczny bez utraty danych formularza.

### Heartbeat — wariant formularza

Harmonogram (cron lub okres) i **grace** jako osobne, wyjaśnione pola; sygnały start / sukces / błąd; kod wyjścia; czas wykonania; wiek ostatniego sukcesu. Zaraz po utworzeniu gotowe snippety (curl, skrypt powłoki z ograniczonymi ponowieniami i przekazaniem kodu wyjścia).

Wyjaśnić wprost: **„spóźniony”** (po terminie, w grace — bez alertu) vs **„powinien alarmować”** (grace minął).

## 4.4 Weryfikacja własności celu (chmura)

Kontynuacja dodawania checka — kreator rozwija się pod polem celu.

| Cel | Metody |
|---|---|
| Domena | Rekord DNS TXT lub plik `/.well-known/` (obejmuje subdomeny) |
| Samo IP (ping) | Gotowe `curl` z maszyny docelowej, osobno IPv4 i IPv6 |
| IP z HTTP | `curl` z maszyny albo token na wskazanym porcie i ścieżce |
| Email | Link potwierdzający |

Przebieg: rozpoznaj cel → zaproponuj metodę → instrukcja krok po kroku z przyciskami kopiowania → **Sprawdź teraz** → dokładny wynik.

„Sprawdź teraz” pokazuje **co faktycznie odczytano**, np. „Rekord TXT dla `_verify.example.com` nie pasuje (odczytano `v=spf1…`). Zaktualizuj wpis lub odczekaj na propagację DNS i kliknij «Sprawdź teraz»”. Rozróżniane błędy: brak rekordu, zły token, propagacja, zły adres, zablokowany port, wygasły token, odrzucone wywołanie, pomylone IPv4/IPv6.

Przy problemie z propagacją można zapisać check jako **oczekujący na weryfikację** (nie wykonuje się) albo w trybie z ograniczeniami, jeśli administrator go dopuścił — bez utraty formularza. Tryb z ograniczeniami wypisuje dokładne limity (dłuższy interwał, limit liczby).

Ponowna weryfikacja co 30 dni: ostrzeżenie → okres łaski → pauza z powodem. Dla weryfikacji IP wywołaniem: stały adres/komenda do wpisania w cron.

## 4.5 Szczegóły checka

**Nagłówek:** nazwa, StatusBadge, decyzja z lokalizacji lub powód UNKNOWN, wiek ostatniego sprawdzenia, akcje: Sprawdź teraz, Pauza, Wycisz, Klonuj, Edytuj. Dla „do sprawdzenia konfiguracji” zaraz pod nagłówkiem karta z konkretną podpowiedzią diagnostyczną. Jeśli konfiguracja różni się od pliku, jest pasek informacyjny z przejściem do diffu.

**Sekcje / zakładki:**
1. Wykres czasu odpowiedzi w fazach (DNS, TCP, TLS, TTFB) per lokalizacja, z linią progu; zakresy 24 h / 7 / 30 / 60 / 365 dni (w granicach planu), zakres w URL.
2. Uptime za okres, bez okresów oznaczonych jako fałszywe alarmy (patrz 12.3 — jak liczyć UNKNOWN).
3. Lokalizacje: wynik i dostępność każdej.
4. Zdarzenia i błędy: kategoria, zakres czasu, pełny request/response po rozwinięciu.
5. Historia powiadomień: kanał, próby, fallback, stan końcowy.
6. Historia konfiguracji: zmiany z UI, API, pliku (autor, czas).
7. Diagnostyka typu: certyfikat (wystawca, łańcuch, daty); heartbeat (odrzucone pingi przez limit, pingi odtworzone z bufora po awarii monitoringu).

Nie ładować całej historii wykonań; wykres prowadzi do szczegółów wybranego przedziału. Nowy check: zamiast pustego wykresu „Oczekuje na pierwsze sprawdzenie” z odliczaniem.

## 4.6 Lokalizacje i agenci (`Ustawienia → Lokalizacje i agenci`)

**Tabela agentów:** stan łączenia (połączony / ponawia / uśpiony), wersja (odznaka „Dostępna nowa wersja”, aktualizacja ręczna), ostatni kontakt, ostatni błąd, opóźnienie wykonania, bufor, odsetek błędów sieci.

**Dodanie agenta:** nazwa i lokalizacja → jednorazowy token → gotowa komenda instalacji → oczekiwanie na żywo na połączenie → **pierwszy check wykonany przez agenta**. Onboarding nie kończy się na samym połączeniu.

**Lokalna lista dozwolonych celów:** przechowywana przez agenta, serwer nie może jej zmienić. Check spoza listy dostaje „Do sprawdzenia konfiguracji” z komunikatem na checku i na karcie agenta:

> „Agent *nas-01* odrzucił ten check: adres nie jest na lokalnej liście dozwolonych celów. Ze względów bezpieczeństwa listę zmienia się tylko w pliku konfiguracyjnym agenta — dopisz cel i kliknij «Sprawdź teraz».”

Agent offline / problem z siecią agenta → **jeden alert** o agencie, zależne checki → UNKNOWN, nie fala DOWN.

## 4.7 Alerty i powiadomienia (`Ustawienia → Powiadomienia`)

Cztery pojęcia: **Kanał** (gdzie), **Polityka** (kiedy i komu), **Alert** (problem wymagający uwagi), **ACK** (przejęcie odpowiedzialności).

Podzakładki: **Kanały**, **Polityki**, **Log dostarczeń**.

**Kanały** — MVP: email, webhook z szablonem (edytor + podgląd), Pushover. v1: ntfy, Telegram, Slack, Discord, Teams. Każdy kanał: nazwa, stan (aktywny / wymaga uwagi / martwy / nie skonfigurowano), ostatni test, ostatnia udana dostawa, ostatni błąd, „Wyślij test”, potwierdzenie odbiorcy. Zapis konfiguracji ≠ działające dostarczanie.

**Polityki** — kreator: jakie zdarzenia (DOWN, DEGRADED, UNKNOWN, awaria monitoringu, agent offline, powrót do UP), opóźnienie, do kogo i jakim kanałem, powtarzanie do ACK z limitem, kanał zapasowy. MVP: okno alertu zbiorczego. v1: ciche godziny, eskalacje.

**Limit powiadomień planu:** baner „Limit powiadomień osiągnięty”, nigdy ciche przestanie wysyłać.

**ACK / snooze / wyciszenie:** ACK zatrzymuje powtarzanie/eskalację zgodnie z polityką, nie rozwiązuje niczego. Snooze i wyciszenie zawsze mają czas i zakres.

**Bezpieczeństwo linku ACK:** link z emaila/pusha jest podpisany i wygasa, a **GET tylko otwiera stronę potwierdzenia**, właściwe ACK to POST jednym przyciskiem. Skanery linków w poczcie (np. Safe Links) otwierają GET-y i bez tego potwierdzałyby alerty automatycznie.

Treść powiadomień: sekcja 9.5.

## 4.8 Incydenty i planned work

**Lista** (wspólna, filtr `kind`): na górze przypięte trwające incydenty (w tym automatyczne szkice do zatwierdzenia) i trwające okna prac; dalej historia. Kolumny: status, dotknięte komponenty, wpływ, czas trwania, właściciel/ACK, czy opublikowany.

**Cykl incydentu:** Badanie → Zidentyfikowano → Monitorowanie → Rozwiązano. Automatyczne incydenty przy DOWN: publikowane od razu albo jako szkic.

**Widok szczegółowy — wspólny dla incydentu i planned work:**
- lewa kolumna: tytuł, pasek postępu statusu, **kompozytor aktualizacji na górze** (przełącznik: wewnętrzna / opublikuj na status page; miejsce na szablony i powiadomienie subskrybentów w v1), pod nim oś zdarzeń — najnowsze na górze;
- prawa kolumna (PropertiesAside): dotknięte komponenty, wpływ, tryb publikacji, właściciel; dla planned work: jednorazowe/cykliczne, strefa czasowa, „wlicz do SLA”. Pola bezramkowe, obrys na hover/focus;
- telefon: kompozytor przypięty nad osią, właściwości w zwijanym arkuszu.

**Oś zdarzeń:** aktualizacje ludzi jako wyraźne karty (autor, czas, pełne zdania), zmiany statusu jako krótkie linie, zdarzenia systemowe jako zwinięte jednowierszowce zapisane zdaniami, powtarzalne zdarzenia techniczne grupowane, szczegóły po rozwinięciu. To wzorzec OpenStatus po czterech iteracjach: płaska oś wyglądała jak plik logów.

**Post-mortem:** SafeRichText. **Fałszywe alarmy:** pojedynczo i masowo dla przedziału czasu.

**Model (rekomendacja MVP):** jeden obiekt Incident z aktualizacjami prywatnymi lub opublikowanymi na wybranej status page. Incydent może istnieć bez publikacji. Rozdzielenie modeli dopiero, gdy cykle życia się rozejdą.

**Planned work:** okna jednorazowe i cykliczne, komponenty, „wlicz do SLA”, cykl Zaplanowane → W toku → Weryfikacja → Zakończone, automatyczny start/koniec, wcześniejsze zakończenie i **przedłużenie**. Czas pokazany jednocześnie w strefie okna i w strefie przeglądarki operatora. Jeśli pod koniec okna checki wciąż zwracają błąd, pojawia się łagodne ostrzeżenie z opcją przedłużenia, żeby po wygaśnięciu nie było nagłego alarmu. Monitoring działa dalej, MAINTENANCE tylko wycisza alerty.

**Puste stany:** „Brak incydentów” + „Zgłoś incydent” / „Zaplanuj prace”.

## 4.9 Status pages — zarządzanie

Lista: nazwa, widoczność, domena, liczba komponentów, ogólny stan, publikacja/zawieszenie (w chmurze baner z powodem zawieszenia).

Edytor — trzy zakładki:
1. **Struktura:** grupy i komponenty; komponent = publiczna nazwa, powiązane checki, reguła agregacji, ręczne nadpisanie stanu, kolejność. Nie przenosić automatycznie wewnętrznych nazw checków.
2. **Wygląd:** logo, kolory z automatyczną kontrolą kontrastu (blokada lub ostrzeżenie). **Kolory marki nie nadpisują kolorów stanów** — marka to akcent i nagłówek, semantyka stanów jest stała.
3. **Podgląd:** ten sam renderer co strona publiczna.

v1: własna domena, prywatna strona, subskrypcje, badge’e, RSS.

## 4.10 Publiczna status page

Cel: pełna sytuacja w ~3 sekundy, na telefonie.

**Hierarchia:**
1. Logo i nazwa.
2. Baner stanu ogólnego (najgorszy stan komponentów lub stan z ręcznego incydentu).
3. Czas ostatniej aktualizacji; przy danych z kopii baner trybu awaryjnego z wiekiem danych.
4. **Aktywne incydenty i trwające prace — zawsze nad komponentami** (czas rozpoczęcia, najnowsza aktualizacja).
5. Komponenty i grupy (grupy zwijane, przy długich listach domyślnie zwinięte zdrowe) z paskiem historii 90 dni.
6. Nadchodzące planned work.
7. Poprzednie incydenty.
8. Stopka: PL/EN, motyw, „Kod źródłowy”, „Zgłoś nadużycie” (chmura).

**Pasek historii:**
- dzień z awarią oznaczany od razu, także bieżący. Bez progów, przez które słupek zostaje zielony mimo trwającej awarii — na to skarżą się użytkownicy Atlassian Statuspage;
- proporcja awarii w ciągu dnia (intraday);
- tooltip / tap: data, minuty niedostępności, stan, powiązany incydent;
- dni z problemem mają dodatkowy znacznik (kropka/kreskowanie), nie tylko kolor.

**Cache:** strona serwowana z ostatniego snapshotu (np. statyczny plik / CDN), który przeżywa awarię API. Snapshot zawiera znacznik „wygenerowano o” w UTC, wiek liczy klient. Historyczny UP nigdy nie jest pokazywany jako aktualny.

Nie ujawniać nagłówków requestów, wewnętrznych adresów ani diagnostyki.

**Stany:** ładowanie — szkielet nagłówka i 3 komponentów; nowa strona bez komponentów — informacja dla administratora (w podglądzie), nie pusta biel.

v1: subskrypcje email/webhook z filtrem komponentów, RSS/Atom.

## 4.11 Ustawienia przestrzeni

Pogrupowane, żeby było mniej zakładek:

| Zakładka | Zawartość |
|---|---|
| Ogólne i domyślne | Nazwa, język, strefa czasowa, wartości domyślne ze źródłem |
| Zweryfikowane cele | Stan i terminy weryfikacji |
| Automatyzacja | Tryb incydentów automatycznych, okno alertu zbiorczego |
| Powiadomienia | Kanały, polityki, log dostarczeń |
| Lokalizacje i agenci | Instalacja, diagnostyka, dozwolone cele |
| Zespół i logowanie | Role Admin / Edytor / Podgląd, zaproszenia, OIDC (w chmurze własne OIDC przestrzeni) |
| Tokeny API i sekrety | Tokeny: zakres, unieważnianie. Sekrety: zawsze zamaskowane, tylko „Podmień” |
| Plan i limity | Zużycie, ograniczenia |
| Konfiguracja | Eksport do pliku, lista różnic, import/uzgadnianie |
| Audyt | Historia zmian |
| Eksport i usunięcie | Operacje na przestrzeni (z potwierdzeniem nazwą) |

Wyszukiwarka ustawień, gdy opcji przybędzie.

## 4.12 Administracja instancji

Oddzielona od ustawień przestrzeni.

**Stan systemu** (siatka kart; na telefonie jedna kolumna „Zdrowie systemu teraz”, najpierw problemy): wersja, lider, opóźnienie wykonania, wejście heartbeatów, obciążenie, liczba alertów względem normy, elementy uśpione, zaległości archiwum, watchdog, ostatni zrzut, wynik testu odtworzenia.

**Obszary:** przestrzenie i plany; rejestracja (zaproszenia, lista oczekujących); sposoby logowania; polityka weryfikacji celów; zgłoszenia nadużyć i zawieszanie stron; lokalizacje i cele kontrolne; limity wejścia; polityka ponowień; polityka ruchu wychodzącego; porzucone checki; klucz szyfrowania (rotacja, stan kopii); zrzuty bazy.

**Krytyczne akcje** (zawieszenie strony, rotacja klucza): modal z domeną i nazwą przestrzeni, wymóg wpisania nazwy, wybór powodu, opis konsekwencji.

## 4.13 Logowanie i pierwsze uruchomienie

**Self-host:** logowanie przez OIDC skonfigurowane w instancji (sposób wskazania pierwszego Admina do ustalenia; lokalny admin i 2FA później, ADR 0006) → podstawowe ustawienia → pierwszy check → kanał → test dostarczenia → aktywacja polityki.

**Chmura:** GitHub / Google (link w emailu później) → kod zaproszenia lub lista oczekujących → przestrzeń → jasna informacja o ograniczeniach planu darmowego („jedna lokalizacja, bez SLA”) → pierwszy check → weryfikacja → kanał i test.

**Kolejność:** nie blokować pierwszego checka wymogiem kanału. Ścieżka: check → kanał → „Wyślij test” → polityka → test całego przepływu. Puste stany zawsze wskazują następną czynność.

---

# 5. Kluczowe przepływy

Każdy przepływ ma punkty tarcia (⚠) i rozwiązania (→).

### 5.1 Od zera do pierwszego alertu na telefonie
Start (self-host: logowanie OIDC; chmura: logowanie + info o planie) → pierwszy check: adres, Testuj, Zapisz → weryfikacja (chmura) → kanał (Pushover/email) → „Wyślij test” → polityka → test łańcucha.  
⚠ Check zapisany, ale brak kanału lub polityki. → Podpowiedź „Dodaj kanał” w formularzu checka; Przegląd pokazuje „Alerty nie trafią do nikogo — brak aktywnej polityki”.  
⚠ Propagacja DNS, zły odbiorca, spam, osiągnięty limit planu. → Dokładne komunikaty, status potwierdzenia odbiorcy.

### 5.2 Check na cel wymagający weryfikacji
Wpisanie celu → kreator pod polem → metoda (TXT / well-known / curl IPv4/IPv6 / token / email) → kopiowanie → Sprawdź teraz → wynik → test checka → zapis.  
⚠ Propagacja DNS, zablokowany port, pomylone IPv4/IPv6. → Odczytana wartość/błąd w komunikacie; zapis jako oczekujący bez utraty formularza.

### 5.3 Agent za firewallem
Token i komenda → uruchomienie → „Połączony” → przypisanie checka → (cel spoza allowlisty → „Do sprawdzenia konfiguracji” z powodem) → dopisanie celu w pliku agenta → Sprawdź teraz → UP.  
⚠ Brak ruchu wychodzącego, DNS/TLS; szukanie allowlisty w panelu WWW. → Komunikat wprost: lista jest tylko lokalna, wskazany agent i plik.

### 5.4 Alert w nocy
Push (tytuł + treść wg 9.5) → ACK z ekranu blokady lub po otwarciu linku → szczegóły checka (faza błędu, ostatnia odpowiedź, runbook) → „Utwórz incydent” lub zatwierdź szkic → aktualizacja w kompozytorze → opcjonalna publikacja → Rozwiąż dopiero po naprawie.  
⚠ Głęboka nawigacja, drobne przyciski w ciemności, wygasły link, mylenie ACK z Resolve, brak uprawnień do publikacji. → „Potwierdź” i „Utwórz incydent” 44–48 px na górze widoku mobilnego, ciemny motyw wg systemu, ACK wyraźnie opisane jako „przejmuję”, Resolve osobno.

### 5.5 Globalna awaria dostawcy — 200 checków
Okno alertu zbiorczego → jedno powiadomienie `[DOWN ×200] …` z pierwszymi pozycjami → Przegląd → „Wycisz masowo” (grupa / tag / filtr DOWN, np. 1 h, z liczbą) → po fakcie masowe oznaczenie przedziału jako fałszywy alarm.  
⚠ Brak wspólnego tagu, za krótkie okno, wyciszenie bezterminowe, przeoczona niezależna awaria. → Wymagany czas wygaśnięcia, podgląd liczby, wyciszone nowe DOWN nadal widoczne w kolejce. Korelacja (v1) zawsze jako „możliwa wspólna przyczyna”.

### 5.6 Awaria samego monitoringu
Wykrycie (ingress / agent / lokalizacja / baza) → zależne checki → UNKNOWN (łupek) → baner z przyczyną i wiekiem danych → jeden alert systemowy → po powrocie przetworzenie wyników z bufora (widoczne na osi checka).  
⚠ Użytkownik bierze nie-zieloną listę za awarię usług. → Łupek zamiast czerwieni + baner mówiący wprost, że usterka jest po stronie monitoringu.

### 5.7 Planned work
Komponenty → okno (jednorazowe/cykliczne) → czas w dwóch strefach → „wlicz do SLA” → podgląd komunikatu publicznego → zaplanowanie → widoczność na status page → automatyczny start (MAINTENANCE) → koniec / wcześniejsze zakończenie / przedłużenie.  
⚠ Pomylona strefa czasowa, wrażenie, że monitoring stoi, alarm tuż po końcu okna. → Podwójna strefa, tekst „monitoring działa, alerty wyciszone”, ostrzeżenie przed końcem.

### 5.8 Konfiguracja w Git
Zmiana w UI → baner różnicy (globalnie + ikona przy checku) → Ustawienia → Konfiguracja → lista różnic → eksport fragmentu → commit → wdrożenie. Gdy CI wdroży starszy plik → konflikt (wartość bazowa, aktywna z UI wraz z autorem i datą, nowa z pliku) → jawny wybór per pole: „Zastosuj wartość z pliku” / „Zachowaj wartość z UI”, podgląd wyniku przed zastosowaniem.  
⚠ Plik po cichu nadpisuje nocną poprawkę z UI. → **Nigdy cichego last-write-wins.**

### 5.9 Przekroczenie limitu planu
Ostrzeżenie przy zbliżaniu się → po przekroczeniu baner z odliczaniem okresu na reakcję → lista zasobów zużywających limit → ręczny wybór checków do pauzy (jeśli plan pozwala) → automatyczna pauza z powodem „Przekroczony limit planu” → przywrócenie przyciskiem w banerze po zmianie planu/zużycia.  
⚠ Nie wiadomo, co przestało działać. → Baner prowadzi do listy z filtrem `paused_reason=plan_limit`.

### 5.10 Odwiedzający status page w trakcie awarii
Pierwszy ekran telefonu: baner stanu, otwarty incydent z czasem i najnowszą aktualizacją, kluczowe komponenty, wiek danych przy cache.  
⚠ 40 komponentów spycha incydent poza ekran; ciężka strona; stary UP jako aktualny. → Incydenty nad komponentami, zwijane grupy, lekki shell, reguła 2.3.

### 5.11 Administrator w podróży
Administracja na telefonie → karty „Zdrowie systemu teraz” → zgłoszenie nadużycia → szczegóły → zawieszenie z powodem.  
⚠ Pomyłka strony na małym ekranie. → Modal z domeną, nazwą przestrzeni i wpisaniem nazwy.

---

# 6. Wspólne komponenty

Cel: jak najmniej komponentów. Nazwy to kontrakty, nie obowiązkowe nazewnictwo w kodzie.

| Komponent | Odpowiedzialność | Gdzie |
|---|---|---|
| `StatusBadge` | Jeden z 5 stanów: kolor + ikona + tekst. Bez czasu i metadanych | Wszędzie, też status page |
| `FlagTag` | Mikro-odznaki oznaczeń; wizualnie słabsze od stanu, nigdy jak DOWN | Listy, szczegóły |
| `StatusMeta` | Czas trwania, `2/3 lok.`, ostatnia aktualizacja, wiek danych | Listy, szczegóły, status page |
| `SystemBanner` | Awaria monitoringu, różnica z plikiem, limity, tryb awaryjny status page, zawieszenie, utrata live | Panel, status page |
| `ServerDataTable` | Tabela sterowana URL, paginacja serwerowa, karty na telefonie, zaznaczanie strona/wszystko, przypięty pasek akcji masowych | Checki, incydenty, audyt, kanały, przestrzenie |
| `FilterBar` | Wyszukiwanie, filtry fasetowe, sortowanie, kafelki liczników; synchronizacja z URL | Przegląd, Checki, Incydenty, Audyt |
| `ScopeConfirmDialog` | Podgląd zakresu akcji masowej: liczba, kryterium, czas wygaśnięcia, „pokaż objęte”, potwierdzenie | Wyciszanie, akcje masowe, fałszywe alarmy |
| `EventTimeline` | Oś: kompozytor na górze, karty ludzi, jednowierszowce systemu, grupowanie | Incydenty, planned work, publiczne aktualizacje |
| `PropertiesAside` | Pary etykieta–wartość, pola bezramkowe z obrysem na hover/focus | Szczegóły incydentu i prac |
| `HistoryBar` | Pasek historii (90 dni lub inny), proporcja intraday, znacznik nie-kolorowy, tooltip | Status page, podgląd, szczegóły checka |
| `PhaseLatencyChart` | Fazy DNS/TCP/TLS/TTFB per lokalizacja, linia progu, zakres w URL | Szczegóły checka (lista ma zwykły sparkline) |
| `SchemaForm` + `SourceValue` | Formularz z definicji typu; każde pole z wartością efektywną, źródłem i „Przywróć” | Checki, ustawienia domyślne, polityki |
| `ConfigDiff` | Różnice aktywna vs plik; tryb konfliktu z trzema wartościami | Konfiguracja, szczegóły obiektu |
| `VerificationStepper` | Metody weryfikacji, kopiowanie, Sprawdź teraz, odczytany wynik | Formularz checka, Zweryfikowane cele |
| `CopySnippetBox` | Blok kodu z wariantami (curl/skrypt, IPv4/IPv6), maskowaniem sekretów, kopiowaniem | Heartbeat, agent, weryfikacja, API |
| `SafeRichText` | Edytor i renderer bezpiecznego formatowania (nagłówki, listy, linki, kod) | Opisy, aktualizacje, post-mortemy |
| `HealthCard` | Zdrowie agenta, kanału lub elementu systemu: stan, ostatni kontakt/dostawa, ostatni błąd, akcja testowa | Agenci, kanały, Stan systemu |
| `EmptyState` / `ErrorState` | Następna sensowna czynność / co się stało → dlaczego → co zrobić | Wszędzie |

> Względem v1.0 scalono: `AlertBanner`→`SystemBanner`, `BulkActionBar` → część `ServerDataTable`, `TimelineEntry`/`TimelineComposer` → części `EventTimeline`, `AgentHealthCard` + `ChannelHealth` → `HealthCard`, `MaintenanceCard` → wariant wpisu listy incydentów, `FreshnessIndicator` → część `StatusMeta`. Doszły `ScopeConfirmDialog`, `CopySnippetBox`, `SafeRichText` i `PropertiesAside` (z PDF).

## 6.1 Kontrakty

- **StatusBadge:** tylko stan; zawsze tekst + ikona; kolor nigdy jedynym nośnikiem. Wariant kompaktowy (ikona + tooltip) wolno stosować tylko tam, gdzie obok jest etykieta tekstowa wiersza, np. w HistoryBar.
- **FlagTag:** stonowany; ważny powód nie może być ukryty tylko w tooltipie.
- **SourceValue:** wartość efektywna, źródło, wartość bazowa przy override, „Przywróć”; przy różnicy z plikiem dopisek `W pliku: 60 s` i link do diffu.
- **EventTimeline:** najnowsze na górze; ludzie > statusy > system w hierarchii wizualnej; te same typy wpisów dla incydentów i prac.
- **ScopeConfirmDialog:** zawsze liczba obiektów i czas wygaśnięcia; dla szerokich zakresów dodatkowe potwierdzenie.

---

# 7. Konfiguracja i GitOps w UI

**Źródło wartości:** domyślna instancji → planu → przestrzeni → własna obiektu. Zawsze dostępne, wizualnie dyskretne (4.3).

**Różnice — trzy poziomy:**
1. Globalnie: baner „Konfiguracja różni się od pliku”.
2. Lista: ikona pliku z edycją przy nazwie (tooltip „Konfiguracja w UI różni się od pliku”) + filtr „Tylko różniące się od pliku”.
3. Szczegóły: pasek z przejściem do eksportu YAML/JSON; przy polu `W pliku: 300 s`.

| Parametr | Aktywna | Plik |
|---|---:|---:|
| Interwał | 60 s | 300 s |
| Timeout | 10 s | 10 s |

**Konflikt:** wartość bazowa, aktywna (z autorem i datą zmiany), nowa z pliku, historia, podgląd wyniku. Rozwiązanie per pole, jawne. Bez niejawnego last-write-wins.

---

# 8. Tokeny wizualne i dostępność

Kierunek: spokojny, profesjonalny interfejs techniczny. Mało kolorowych kart, bez dekoracyjnych gradientów i zbędnych ramek. Większe karty tylko dla problemów wymagających uwagi.

## 8.1 Kolory stanów

Dwie role koloru:
- **Pigułka (badge, tagi stanów):** ciemny tekst na jasnym tle (jasny motyw) / jasny tekst na ciemnym tle (ciemny motyw). Paleta z v1.0, wyższe kontrasty.
- **Solid (przyciski, segmenty HistoryBar, linie wykresów, kropki):** nasycony kolor z PDF + reguła koloru tekstu na wypełnieniu.

Wszystkie kontrasty poniżej przeliczono wzorem WCAG (sRGB, luminancja względna).

### Pigułki — tryb jasny

| Stan | Tekst | Tło | Kontrast |
|---|---|---|---:|
| UP | `#166534` | `#DCFCE7` | 6,49:1 |
| DEGRADED | `#854D0E` | `#FEF3C7` | 6,15:1 |
| DOWN | `#991B1B` | `#FEE2E2` | 6,80:1 |
| UNKNOWN | `#334155` | `#E2E8F0` | 8,40:1 |
| MAINTENANCE | `#1E40AF` | `#DBEAFE` | 7,15:1 |

### Pigułki — tryb ciemny

| Stan | Tekst | Tło | Kontrast |
|---|---|---|---:|
| UP | `#86EFAC` | `#123322` | 9,82:1 |
| DEGRADED | `#FCD34D` | `#3A2A10` | 9,59:1 |
| DOWN | `#FCA5A5` | `#3B1818` | 8,32:1 |
| UNKNOWN | `#CBD5E1` | `#242C3A` | 9,45:1 |
| MAINTENANCE | `#93C5FD` | `#172B48` | 7,89:1 |

### Solid — tryb jasny (tło strony `#FFFFFF`)

| Stan | Solid | vs tło (≥3:1 grafika) | Tekst na solid | Kontrast tekstu |
|---|---|---:|---|---:|
| UP | `#059669` | 3,77:1 | **ciemny `#0B0F17`** | 5,09:1 |
| DEGRADED | `#D97706` | 3,19:1 | **ciemny `#0B0F17`** | 6,02:1 |
| DOWN | `#E11D48` | 4,70:1 | biały | 4,70:1 |
| UNKNOWN | `#64748B` | 4,76:1 | biały | 4,76:1 |
| MAINTENANCE | `#2563EB` | 5,17:1 | biały | 5,17:1 |

Uwaga: biały tekst na UP `#059669` ma tylko 3,77:1 i nie spełnia 4,5:1. PDF wskazał ten problem tylko dla DEGRADED.

### Solid — tryb ciemny (karta `#111827`)

| Stan | Solid | vs karta | Tekst na solid | Kontrast tekstu |
|---|---|---:|---|---:|
| UP | `#10B981` | 6,99:1 | ciemny `#0B0F17` | 7,56:1 |
| DEGRADED | `#F59E0B` | 8,26:1 | ciemny `#0B0F17` | 8,93:1 |
| DOWN | `#F43F5E` | 4,83:1 | ciemny `#0B0F17` | 5,22:1 |
| UNKNOWN | `#94A3B8` | 6,92:1 | ciemny `#0B0F17` | 7,48:1 |
| MAINTENANCE | `#3B82F6` | 4,82:1 | ciemny `#0B0F17` | 5,21:1 |

**Reguła:** w trybie ciemnym tekst na wypełnieniu zawsze `#0B0F17`. W jasnym: ciemny na UP i DEGRADED, biały na pozostałych.

Daltonizm: przy deuteranopii zieleń i karmin zbliżają się do siebie mimo doboru odcieni, dlatego **ikona + tekst są obowiązkowe**, a na HistoryBar dni z problemem mają znacznik nie-kolorowy. Wartości kontrastu dotyczą wskazanych par, nie gwarantują zgodności całego produktu. Stany hover/focus/disabled trzeba sprawdzić osobno.

## 8.2 Ikony

| Stan | Symbol |
|---|---|
| UP | Kółko z ptaszkiem (check-circle) |
| DEGRADED | Trójkąt ostrzegawczy (alert-triangle) |
| DOWN | **Ośmiokąt z krzyżykiem (octagon-x)** — kształt „stop” odróżnia się od kółek bez koloru |
| UNKNOWN | Przerywane kółko ze znakiem zapytania |
| MAINTENANCE | Klucz (wrench) |
| Nie mierzy (2.5) | Pauza / koło zębate / klepsydra, neutralny szary |

Powód DEGRADED: zegar (wolno), pinezka/globus (część lokalizacji). Ikony oznaczeń nie zastępują ikony stanu.

## 8.3 Typografia

| Element | Rozmiar |
|---|---:|
| Nagłówek strony | 24–28 px |
| Tytuł sekcji | 18–20 px |
| Tytuł komponentu | 15–16 px |
| Tekst główny | 14 px |
| Metadane | 12–13 px |
| Dane techniczne | 12–14 px, **krój monospace** (URL-e, rekordy DNS, logi, snippety) |

Czasy, procenty, liczniki: `font-variant-numeric: tabular-nums`. Polskie etykiety bywają wyraźnie dłuższe, więc przyciski, zakładki i liczniki nie mogą mieć sztywnej szerokości (testować oba języki).

## 8.4 Odstępy

Skala `4 / 8 / 12 / 16 / 24 / 32 px`. Zwarte tabele, umiarkowane odstępy sekcji, mało ramek, bez zagnieżdżania kart.

## 8.5 Dostępność (WCAG 2.2 AA)

- Stan nigdy tylko kolorem; tekst ≥ 4,5:1; duży tekst i elementy graficzne/kontrolki ≥ 3:1 (1.4.11).
- Widoczny pierścień focusu ≥ 3:1; sensowna kolejność tabulacji i nagłówków.
- Etykiety, błędy i komunikaty powiązane z kontrolkami.
- Cel dotyku: minimum WCAG 2.5.8 to 24×24 px. Rekomendacja produktu: 44–48 px na telefonie i przy akcjach kryzysowych.
- Komunikaty statusu na żywo przez `aria-live` (4.1.3), zdławione (sekcja 4).
- Bez samej ikony tam, gdzie znaczenie akcji nie jest oczywiste.

---

# 9. Język PL/EN i powiadomienia

## 9.1 Ton

Rzeczowy, spokojny, bezpośredni. Wyjaśnia problem, nie obwinia użytkownika.

## 9.2 Słownik

| Pojęcie | PL | EN |
|---|---|---|
| UP / DEGRADED / DOWN / UNKNOWN / MAINTENANCE | Działa / Działa z problemami / Nie działa / Stan nieznany / Prace planowe | Operational / Degraded / Down / Unknown / Maintenance |
| ACK | Potwierdź (przejmuję) | Acknowledge |
| Resolve | Rozwiąż incydent | Resolve incident |
| Snooze | Wycisz na… | Snooze for… |
| Paused / Flapping / Unverified | Spauzowany / Niestabilny / Niezweryfikowany | Paused / Flapping / Unverified |
| Pending first check | Oczekuje na pierwsze sprawdzenie | Waiting for first check |

## 9.3 Technika i18n

- **Formy mnogie przez ICU MessageFormat** — polski ma trzy formy („1 lokalizacja”, „2 lokalizacje”, „5 lokalizacji”). Bez sklejania stringów.
- Czas względny i daty przez `Intl` („6 minut temu” / “6 minutes ago”); w powiadomieniach i emailach godzina ze strefą (np. `02:14 CET`).
- Język powiadomienia = język odbiorcy (ustawienie użytkownika), nie autora polityki.

## 9.4 Komunikaty błędów

Schemat: **co się stało → po czyjej stronie / dlaczego → co zrobić teraz.**

- „Odmowa połączenia na porcie 443 z lokalizacji Frankfurt. Cel może blokować ruch zewnętrzny — sprawdź zaporę albo przenieś check do lokalnego agenta.”
- „Agent nie odpowiada od 6 minut.”
- „Nie możemy teraz potwierdzić stanu części checków. Problem leży po stronie monitoringu: [źródło]. Ostatnie wiarygodne dane: [czas].” / “We can't confirm the status of some checks right now. The issue is on the monitoring side: [source]. Last reliable data: [time].”
- „Ten check nie zadziałał jeszcze ani razu. Sprawdź adres, port lub reguły firewalla.”

Bez „Wystąpił nieznany błąd”, gdy przyczyna jest znana. Akcje nazywane jednoznacznie: „Potwierdź”, „Wycisz na 30 minut”, „Wznów monitoring”, „Sprawdź ponownie”, „Rozwiąż incydent”, „Opublikuj aktualizację”, „Zakończ prace”.

## 9.5 Treść powiadomień

Budżet przed ucięciem na ekranie blokady (orientacyjnie, wg zestawienia EngageLab 2026): **iOS ~25–35 znaków tytułu, ~90–120 treści**; Android ~40–50 / ~90–150; web push ~30–45 / ~60–100. Pushover przyjmuje do 250/1024 znaków, ale widoczne jest tylko to, co mieści się na blokadzie. Kolejność informacji: stan → nazwa → błąd i od kiedy → lokalizacje.

| Kanał | Wzór |
|---|---|
| Push — tytuł | `[DOWN] API Prod (3/3 lok.)` · zbiorczy `[DOWN ×12] Płatności` · powrót `[UP] API Prod (po 14 min)` · monitoring `[MONITORING] Brak danych z agenta nas-01` |
| Push — treść | `HTTP 502 od 02:14 CET. FRA, WAW, AMS.` |
| Push — akcje | Potwierdź · Wycisz 30 min (gdzie platforma wspiera); tap zawsze otwiera właściwy check/incydent |
| Email — temat | `[DOWN] API Prod — HTTP 502 (3/3 lok.)` |
| Email — pierwsze linie | Linki: Potwierdź (zatrzymaj powtarzanie) · Szczegóły · Utwórz incydent; zrzuty odpowiedzi i runbook niżej |
| Webhook | Pełny JSON: stan, powód, lokalizacje, czasy, linki, `degradedReason`/powód UNKNOWN |

Kod stanu w nawiasie w tytule to celowy wyjątek od zasady „etykieta dla człowieka”: jest krótki, skanowalny i taki sam w PL i EN.

Ograniczenia platform:
- **Pushover nie ma własnych przycisków akcji.** Ma `url` (supplementary URL) do strony ACK. Do rozważenia: dla krytycznych polityk priorytet emergency (2), który wymusza potwierdzenie w aplikacji Pushover i może wywołać callback URL → mapowanie na nasze ACK **[PZ]**.
- Akcje w web push działają nie na wszystkich platformach (iOS je ogranicza), więc ścieżka „tap → strona z dużym przyciskiem Potwierdź” musi działać zawsze.
- W pushu nigdy logów ani payloadów.

---

# 10. Research konkurencji — wnioski

Problemy zgłaszane przez pojedynczych użytkowników to sygnały, nie uniwersalne wady.

| Narzędzie | Co cenić | Problemy / ograniczenia | Wniosek |
|---|---|---|---|
| Uptime Kuma | Szybki start, czytelny wygląd przy małej skali | Zgłoszenia od ~15 s do ponad 50 s ładowania/operacji przy ~700–1 850 monitorach; brak ról; przeładowany układ; problemy z cyklicznym maintenance; wewnętrzne API ≠ wspierany kontrakt | Paginacja i agregacja serwerowa od początku; Przegląd bez historii; role w modelu |
| Healthchecks.io | Model New / Up / Late / Down, rozdział Period i Grace, cisza, logowanie linkiem, przypomnienia o trwających awariach | Brak modelowania zależności | Dedykowany formularz heartbeat, stan „oczekuje”, snippety z kodem wyjścia, digest [PZ] |
| Gatus | YAML/GitOps, wg relacji użytkownika ~4 500 checków na Postgresie, wykrywanie utraty sieci samego monitora | Dashboard = status page; brak osi incydentów i subskrybentów | Diagnostyka własnej sieci w rdzeniu UI; rozdział panelu i status page; UI pokazuje konfigurację efektywną i diff |
| OpenStatus | Komponent status page ≠ monitor; oś incydentu po 4 iteracjach (kompozytor u góry, karty ludzi vs jednowierszowce); jeden zestaw komponentów dla incydentów i maintenance; rozdział incydent / status report | Pierwsze wersje: oś jak log, nadmiar ikon, zbyt masywne przyciski | EventTimeline; widoczność aktualizacji bez dwóch modeli w MVP |
| Better Stack | (wg producenta) grupowanie równoległych incydentów, przejście aktualizacji na ręczne po edycji człowieka, widżet historii intraday | Brak niezależnych ocen; pozwala na własny CSS/JS (sprzeczne z naszymi zasadami) | Okno alertu zbiorczego, ACK jednym dotknięciem, ochrona ręcznych komunikatów [PZ] |
| Atlassian Statuspage | Degraded / Partial / Major, najgorszy stan w nagłówku, pasek 90 dni | Słupek dnia zostaje zielony/żółty mimo trwającego Major Outage (progi minutowe) | Natychmiastowe oznaczanie dnia z awarią + tooltip z minutami |
| Grafana IRM (OnCall OSS zarchiwizowany) | Acknowledge i Silence 30 min w pushu, klikalne liczniki zsynchronizowane z filtrem | Celowy brak „Ack all” w aplikacji mobilnej | Akcje w powiadomieniu; ograniczenie szerokich akcji na telefonie [PZ] |
| PagerDuty | Ponowne wywołanie potwierdzonego, nierozwiązanego incydentu po czasie | — | Wygasanie ACK w politykach v1 [PZ] |
| UptimeRobot, Oh Dear, Instatus | Push, testy powiadomień, akcje masowe, szablony komunikatów | Brak niezależnych recenzji w zebranym materiale | Wzorce przejęte tylko tam, gdzie potwierdzone gdzie indziej |

**Ograniczenia researchu:** głównie dokumentacja, changelogi, issues i dyskusje; nie wszystko testowane na żywo. Twierdzenie o „braku API” w Uptime Kuma nie jest potwierdzone (istnieje wewnętrzne API). Liczby kontrastu z 8.1 przeliczone; metryki „dystansu barw” z PDF pominięte jako niestandardowe. Źródła sprawdzić ponownie przed publikacją zewnętrzną.

---

# 11. Odpowiedzi na pytania z briefu (sekcja 8)

1. **UNKNOWN z naszej winy na status page** → neutralny łupkowy UNKNOWN z ikoną `?` i etykietą „Stan nieznany (problem z pomiarem)”, baner z wiekiem ostatnich pewnych danych, ostatni potwierdzony stan wyłącznie jako historyczny. Ręczny incydent/nadpisanie ma pierwszeństwo. (2.3)
2. **Kworum** → `2/3 lok.` na liście, pełne zdanie w szczegółach, „Ile lokalizacji musi potwierdzić awarię” w formularzu. (2.4)
3. **DEGRADED wolno vs część lokalizacji** → jeden stan i kolor, dwie podetykiety z ikoną (zegar / lokalizacja) + `degradedReason` w API. (2.2)
4. **Źródło wartości** → stonowany podpis pod polem, wyraźny dopiero przy override, „Przywróć”, podsumowanie w nagłówku akordeonu. (4.3)
5. **Różnica UI vs plik** → ikona przy nazwie + filtr na liście; pasek + `W pliku: …` przy polu w szczegółach. (7)
6. **Oznaczenia vs stany przy 5 000** → kolumna stanu tylko dla 5 stanów (wyjątek: neutralna ikona dla nie-mierzących), reszta jako maks. dwie mikro-odznaki przy nazwie. (2.5)
7. **Kolory** → dwie role (pigułka / solid) z przeliczonym kontrastem, reguła tekstu na wypełnieniu, obowiązkowa ikona (DOWN = ośmiokąt) i znacznik nie-kolorowy na pasku historii. (8.1–8.2)
8. **Push** → ~25–35 znaków tytułu, ~90–120 treści na iOS: stan, nazwa, błąd + godzina, lokalizacje; akcje tam, gdzie platforma pozwala. (9.5)
9. **Telefon** → macierz w 3.3: pełne operacje kryzysowe, złożone konfiguratory jako podgląd.

---

# 12. Propozycje zmian i decyzje do podjęcia

## 12.1 Wysoki priorytet (MVP)

| Propozycja | Uzasadnienie |
|---|---|
| Jawny powód każdego UNKNOWN w API | Odróżnia awarię monitoringu od awarii usługi |
| `degradedReason` w API | Wolno vs część lokalizacji bez zgadywania |
| Wiek danych + snapshot status page | Nigdy nieaktualny zielony UP |
| Jawny stan synchronizacji konfiguracji | Bez cichych konfliktów UI/plik |
| Test kanału w onboardingu + ostrzeżenie „brak aktywnej polityki” | Użytkownik faktycznie dostaje alerty |
| Wyciszenie zawsze z czasem wygaśnięcia; wyciszone nowe DOWN widoczne w kolejce | Bez przypadkowego bezterminowego uciszenia i przeoczonej niezależnej awarii |
| Powód każdej pauzy (filtrowalny) | Wiadomo, czy to plan, weryfikacja czy ręczna akcja |
| ACK przez POST z linku (GET = strona potwierdzenia) | Skanery linków nie potwierdzają alertów |
| Stan „oczekuje na pierwsze sprawdzenie” w API | Nowy check nie udaje UNKNOWN ani DOWN |
| Powiadomienie o powrocie do UP (z czasem trwania) jako zdarzenie polityki | Zamyka pętlę po nocnym alercie; brief tego wprost nie wymienia |

## 12.2 Do rozważenia (v1)

1. **Wygasanie ACK / ponowne przypomnienie** („Przypomnij po 60 min, jeśli nadal DOWN”) — wzorzec PagerDuty; chroni solo admina, który kliknął ACK i zasnął.
2. **Digest trwających awarii** (dzienny/godzinny email o checkach długo w DOWN/UNKNOWN, także wyciszonych) — wzorzec Healthchecks.io.
3. **Szerokie akcje na telefonie** — brief wymaga zakresu „wszystko”. Propozycja: na telefonie zakres „wszystko” dostępny tylko po wpisaniu liczby objętych checków lub słowa potwierdzającego; domyślnie podpowiadane grupa / tag / alert zbiorczy. (Grafana celowo nie ma „Ack all” na mobile).
4. **Przejście incydentu automatycznego w ręczny po pierwszej aktualizacji człowieka** — automat może co najwyżej zmienić status na „Monitorowanie”, zamknięcie należy do człowieka (wzorzec Better Stack).
5. **Pushover emergency priority ↔ ACK** przez callback.
6. **Wspólna przyczyna awarii** (korelacja po zależnościach, tagach, lokalizacjach) — zawsze „możliwa wspólna przyczyna”.
7. **Lekki widok zależności checków** (wspólny dostawca/komponent).
8. **Priorytet alertu niezależny od stanu** (prod vs test).
9. **Test całego łańcucha:** check → polityka → kanał → odbiorca → ACK.
10. **Szablony aktualizacji** incydentu, maintenance, rozwiązania.

## 12.3 Decyzje wymagane przed implementacją

- Jeden model Incident z aktualizacjami prywatnymi/publicznymi? **Rekomendacja: tak.**
- `degradedReason` i powód UNKNOWN w API? **Rekomendacja: tak.**
- Reguła backendu: kiedy dokładnie UNKNOWN przy awarii monitoringu i jak długo wolno używać danych z cache.
- **Jak liczyć uptime przy UNKNOWN?** Rekomendacja: okresy UNKNOWN wyłączone z mianownika i pokazane osobno jako „pokrycie danymi: 99,2%”, żeby nasza awaria nie psuła ani nie zawyżała uptime’u klienta. Analogicznie MAINTENANCE wg flagi „wlicz do SLA”.
- Próg „szerokiej” akcji wymagającej dodatkowego potwierdzenia (np. > 20 checków lub zakres „wszystko”).
- Zachowanie zakresu „wszystko” na telefonie (12.2 pkt 3).
- Które ustawienia są tylko do podglądu na telefonie (macierz 3.3).

---

# 13. Priorytety projektowania

**P0 — fundament:** model stanów/powodów/oznaczeń; dwa shelle; nawigacja desktop/mobile; Przegląd (triage); lista checków (paginacja, filtry, URL, akcje masowe, ScopeConfirmDialog); formularz checka i SchemaForm; weryfikacja celu; szczegóły checka; kanały, polityki, ACK, treść powiadomień; incydenty i EventTimeline; publiczna status page ze snapshotem i wiekiem danych; awaria monitoringu; źródła wartości i diff.

**P1 — kompletność:** pełna diagnostyka agentów; planned work; Administracja instancji; role i zaproszenia; fallback i log dostarczeń; historia konfiguracji; mobilna administracja.

**P2 — rozwinięcie:** korelacja wspólnej przyczyny; zagnieżdżone grupy; ciche godziny i eskalacje; wygasanie ACK; digest; szablony; subskrypcje, RSS/Atom, badge’e, własna domena.

---

# 14. Skrót decyzji dla implementacji

| Obszar | Decyzja |
|---|---|
| Frontend | React + Vite, dwa shelle: panel i status page |
| Nawigacja | Desktop: 5 sekcji + odseparowana Administracja. Telefon: Przegląd, Checki, Incydenty, Więcej |
| Przegląd | Banery → jedna kolejka problemów z ACK inline → liczniki → zwinięte sekcje → feed |
| Lista checków | Serwerowo: paginacja, filtry, sortowanie, agregacja; URL; brak reorderu pod kursorem |
| Stany | 5 bazowych; nie-mierzące checki = neutralna ikona; oznaczenia jako mikro-odznaki |
| UNKNOWN | Łupek, powód, wiek danych; ostatni stan tylko historycznie; ręczny stan wygrywa |
| DEGRADED | Jeden stan, dwie podetykiety, `degradedReason` |
| Kworum | „X z Y lokalizacji” / `2/3 lok.` |
| Formularze | Progressive disclosure, SchemaForm, dyskretne źródła wartości |
| Heartbeat | Dedykowany wariant: harmonogram, grace, sygnały, snippety |
| Weryfikacja | Kreator w formularzu, odczytany wynik w błędzie, zapis jako oczekujący |
| Agenci | Onboarding do pierwszego wyniku; allowlista jako ograniczenie agenta, nie awaria celu |
| Alerty | Kanał / polityka / alert / ACK rozdzielone; zbiorczy; limit jawny |
| ACK | Przejęcie odpowiedzialności; link → POST; nie rozwiązuje |
| Wyciszenie | Zawsze zakres + liczba + czas wygaśnięcia; bez „na zawsze” |
| Powiadomienia | `[STAN] Nazwa (X/Y lok.)` + błąd i godzina na początku; deep link zawsze |
| Incydenty | Wspólny widok z planned work: kompozytor u góry, oś hierarchiczna, PropertiesAside |
| Status page | Mobile-first, incydenty nad komponentami, snapshot + wiek danych, historia bez fałszywej zieleni |
| GitOps | Diff na 3 poziomach; konflikt jawny per pole; bez last-write-wins |
| Kolory | Pigułka + solid, przeliczony kontrast, ikona obowiązkowa, marka nie nadpisuje stanów |
| i18n | ICU plurals, `Intl`, elastyczne szerokości, język odbiorcy w powiadomieniach |
| Dostępność | WCAG 2.2 AA, tekst + ikona + kolor, focus, `aria-live` zdławione |

---

# 15. Rejestr zmian v1.0 → v1.1

**Dodane z raportu PDF:** solid-owe tokeny kolorów i reguła tekstu na wypełnieniu; ikona DOWN = ośmiokąt; podetykiety DEGRADED z wartościami; `2/3 lok.`; wzorzec dyskretnego źródła wartości z „Przywróć”; ikona diffu + filtr; reguła kolumny stanu dla nie-mierzących checków; brak reorderu na żywo; budżety znaków i wzory powiadomień; dwukolumnowy widok incydentu z PropertiesAside; podwójna strefa czasu i ostrzeżenie przed końcem prac; baner limitu → filtr powodu pauzy; incydenty nad komponentami na status page; natychmiastowe oznaczanie dnia na HistoryBar; potwierdzenie nazwą przy zawieszeniu; „Dodaj kanał” z formularza checka; zapis checka jako oczekującego na weryfikację; parametry URL per sekcja; monospace dla danych technicznych; elastyczne szerokości dla PL; propozycje: wygasanie ACK, digest, ograniczenie szerokich akcji na mobile, auto→ręczny incydent.

**Dodane nowe (luki w obu researchach):** jedna kolejka problemów zamiast dwóch list; stan „oczekuje na pierwsze sprawdzenie”; ACK przez POST (ochrona przed skanerami linków); wyciszone nowe DOWN widoczne w kolejce; powiadomienie o powrocie do UP; brak przycisków akcji w Pushoverze + emergency priority; ICU plurals i język odbiorcy; `aria-live` i `prefers-reduced-motion`; snapshot status page z czasem generowania; kolory marki nie nadpisują stanów; pre-agregowany sparkline; reguła liczenia uptime przy UNKNOWN; poprawka kontrastu solid UP w trybie jasnym; `ScopeConfirmDialog`.

**Usunięte / zmienione:** osobna sekcja „Niepotwierdzone alerty” (scalona); etykieta „Nie można potwierdzić stanu” jako badge (zostaje jako opis); propozycje PDF „Obniżona wydajność” i „Awaria” (rozstrzygnięte w 2.1); liczba „dokładnie 12 komponentów” i metryki „dystansu barw” z PDF; komponenty scalone do 18 (sekcja 6); powtórzona lista źródeł (scalona w jedną poniżej); ustawienia zgrupowane w mniej zakładek.

---

# 16. Źródła

**Brief:** `docs/research/ui-ux-brief.md` — sekcje 3–10; szczegóły w `docs/specs/product-features.md` (v0.9).

**Produkty i dokumentacja**
- Uptime Kuma — https://github.com/louislam/uptime-kuma · issues #6181, #6361, #7079, #6813, #2889, #7616
- Reddit — Uptime Kuma przy 12k usług: https://www.reddit.com/r/UptimeKuma/comments/1qqdydb/ · alternatywy wydajnościowe: https://www.reddit.com/r/selfhosted/comments/1m71gma/ · Gatus vs Uptime Kuma: https://www.reddit.com/r/selfhosted/comments/1b0qzni/ · Gatus UI: https://www.reddit.com/r/selfhosted/comments/1ng69u0/ · fałszywe alerty: https://www.reddit.com/r/homelab/comments/1xkzx6p/
- Healthchecks.io — https://healthchecks.io/docs/ · https://healthchecks.io/docs/monitoring_cron_jobs/ · https://healthchecks.io/faq/
- Gatus — https://gatus.io/docs · https://github.com/TwiN/gatus
- OpenStatus — https://www.openstatus.dev/blog/four-iterations-of-an-incident-timeline · https://www.openstatus.dev/docs/concept/status-reports-and-incidents · https://www.openstatus.dev/guides/what-is-a-status-page
- Better Stack — https://betterstack.com/docs/uptime/ · https://betterstack.com/docs/uptime/acknowledging-an-incident/ · https://betterstack.com/docs/uptime/creating-status-report-and-status-update/
- Atlassian Statuspage — https://support.atlassian.com/statuspage/docs/what-is-statuspage/ · https://support.atlassian.com/statuspage/docs/top-level-status-and-incident-impact-calculations/
- Grafana IRM — https://grafana.com/docs/grafana-cloud/alerting-and-irm/incident/ · https://grafana.com/docs/grafana-cloud/observe-and-act/respond-to-incidents/respond-to-alerts/ · https://grafana.com/docs/grafana-cloud/platform/mobile-app/push-notifications/ · https://github.com/grafana/oncall/issues/2618
- PagerDuty — https://docs.pagerduty.com/incident-management/services/configurable-service-settings
- Pushover API — https://pushover.net/api
- Oh Dear — https://ohdear.app/docs · UptimeRobot — https://uptimerobot.com/ · PingPing — https://pingping.io/guides/status-page-examples
- EngageLab — limity znaków powiadomień 2026: https://www.engagelab.com/blog/push-notification-character-limits

**Standardy**
- WCAG 2.2 — https://www.w3.org/TR/WCAG22/ · 1.4.11 Non-text Contrast — https://appt.org/en/guidelines/wcag/success-criterion-1-4-11
- Apple HIG Notifications — https://developer.apple.com/design/human-interface-guidelines/notifications
- Android Notifications — https://developer.android.com/develop/ui/views/notifications

---

# 17. Końcowa rekomendacja

Produkt ma wyróżniać się trzema rzeczami:

1. **Uczciwość statusu** — prawidłowo rozpoznana awaria monitoringu, wiek danych, brak fałszywego zielonego i fałszywego czerwonego.
2. **Skala bez bólu** — triage zamiast ściany checków, serwerowa paginacja, bezpieczne operacje masowe.
3. **UX incydentu z telefonu** — ACK bez rozwiązywania, hierarchiczna oś, szybka aktualizacja i kontrolowana publikacja.
