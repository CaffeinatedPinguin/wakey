# Funkcje produktu: uptime monitor (v0.9)

Uzupełnia `uptime-monitor-plan.md` (v0.16, poza tym katalogiem). Plan mówi **jak** system jest zbudowany i **kiedy** co wchodzi (sekcja 14), ten dokument mówi **co** każdy komponent robi dla użytkownika.
Oznaczenia: **MVP**, **v1**, **później**.

**Zmiany względem v0.8:**
- **Agent:** Go, Linux + Docker w MVP, bez automatycznych aktualizacji; podpisane wydania i powiadomienie o nowej wersji zostają.
- **Stack:** decyzje backendu, baz i agenta w `decisions/0001-stack.md`; frontend bez zmian (React + Vite, szczegóły w `research/backlog.md` pkt 1.5).

**Zmiany względem v0.7:**
- **Logowanie:** self-host dowolne OIDC i lokalny admin; chmura GitHub, Google, link w emailu, opcjonalnie własne OIDC przestrzeni.
- **Bez własnego HTML nigdzie:** status page tylko z logo i kolorami, treści z bezpiecznym formatowaniem. Zgłaszanie nadużyć i zawieszanie stron.
- **Wersja schematu** pliku konfiguracyjnego i eksportu przestrzeni.
- **Klucz szyfrowania sekretów:** osobna kopia, rotacja, test odtworzenia z odszyfrowaniem.
- **Kworum:** domyślnie większość przy ponad 2 lokalizacjach.
- **Otwarte:** kanały spoza UE (po konsultacji prawnej), wygląd `UNKNOWN` na status page (research UI/UX).

---

## 0. Zasady przekrojowe

### Wszystko konfigurowalne
Każdy limit, próg, interwał, okno czasowe i zachowanie ma wartość domyślną i da się je zmienić. W kodzie nie ma stałych biznesowych.

Konfiguracja ma trzy poziomy. Niższy poziom nadpisuje wyższy, ale tylko w granicach planu:

| Poziom | Kto ustawia | Przykłady |
|---|---|---|
| **Instancja** | właściciel instancji (plik lub zmienne środowiska) | domyślny interwał, okno offline agenta, cele kontrolne, próg wzorca zbiorowego błędu, częstotliwość kontroli wejścia, okres rozruchu, próg opóźnienia wykonania, próg meta-alertu, limity na wejściu, polityka ponowień (limity i odstępy), polityka ruchu wychodzącego, tryb weryfikacji celów, ważność tokenu weryfikacji, okres ponownej weryfikacji, progi porzuconych checków, okres ostrzegawczy limitów, zachowanie przy wdrożeniu z różnicą, dostępne sposoby logowania, częstotliwość zrzutu bazy |
| **Plan** | właściciel instancji | limity checków, minimalny interwał, retencja, liczba agentów i status pages, limit powiadomień w czasie, checki niezweryfikowane (jeśli tryb odblokowany) |
| **Przestrzeń i check** | użytkownik z rolą Edytor lub Admin | interwał, ponowienia (w granicach twardego limitu), kworum (domyślnie „wszystkie” przy 1–2 lokalizacjach, „większość” przy więcej), progi, flapping, polityki alertów, tryb i opóźnienie incydentów |

- UI zawsze pokazuje wartość efektywną i to, skąd pochodzi (np. „300 s, domyślne instancji”).
- Wartość spoza granic planu jest odrzucana przy zapisie z czytelnym komunikatem, a nie po cichu przycinana.

### Nasza awaria to nie awaria użytkownika
Gdy przyczyna leży po stronie monitoringu (wejście heartbeatów, baza, sieć lokalizacji albo agenta, opóźnione wykonanie):
- dotknięte checki dostają `UNKNOWN`;
- alerty dla nich są wstrzymane;
- właściciel dostaje jeden alert o przyczynie.

UI mówi to wprost na banerze. Po powrocie obowiązuje okres rozruchu (domyślnie jeden pełny interwał), zanim system znów oceni brak sygnału.

### Żadnych nieograniczonych ponowień
Każde ponowienie w systemie (checki, powiadomienia, webhooki, archiwum, bufor agenta, weryfikacja, połączenia) przechodzi przez jedną wspólną politykę:
- limit prób albo budżet czasu;
- rosnący odstęp z losowym rozrzutem;
- stan końcowy widoczny w UI i alert.

Ponowne łączenie (agent → API, API → baza) po limicie szybkich prób przechodzi w stan „uśpiony”: jedna próba co 5 minut i alert. Wraca do pracy bez ręcznego restartu.

### Bez własnego HTML
Nikt nie wgrywa własnego HTML, CSS ani skryptów: ani w panelu, ani na status page, ani w szablonach powiadomień. Wygląd status page to logo i kolory. Treści (incydenty, post-mortemy, opisy) to tekst z bezpiecznym formatowaniem.

### Inne zasady
- Każda akcja w UI jest dostępna przez API, a UI jest tylko jednym z jego klientów.
- Każda lista w API jest stronicowana i filtrowana po stronie serwera.
- Każda zmiana trafia do dziennika audytu.
- UI w dwóch językach od startu (PL, EN). Teksty są wydzielone, więc kolejne języki dochodzą bez zmian w kodzie.
- Czas zapisywany w UTC. Strefa czasowa przestrzeni jest używana do wyświetlania i harmonogramów: w wykresach, cronach, oknach serwisowych i raportach.
- Najważniejsze akcje (potwierdzenie, masowe wyciszenie, pauza, stan systemu) działają wygodnie na telefonie.
- Self-host ma pełną funkcjonalność i nie wymaga żadnej usługi zewnętrznej do startu.

---

## 1. Checki

### Pola wspólne dla każdego typu
Nazwa, typ, cel, interwał, timeout, ponowienia (liczba i opóźnienie, w granicach twardego limitu), lokalizacje i próg kworum, tagi, grupa, polityka alertu, opis z linkiem do runbooka, stan (aktywny lub spauzowany, z powodem), stan weryfikacji celu.

### Funkcje
| Funkcja | Etap |
|---|---|
| Dodanie checka: adres i „Zapisz”, reszta zwinięta z domyślnymi wartościami | MVP |
| **Weryfikacja celu przy dodawaniu** (chmura): gdy cel nie jest zweryfikowany, formularz od razu prowadzi przez weryfikację (sekcja 2a) i podpowiada metodę pasującą do typu checka. Jeśli tryb z ograniczeniami jest odblokowany, informuje o nim | MVP |
| **„Testuj” przed zapisem:** jednorazowe wykonanie z pełnym wynikiem | MVP |
| „Sprawdź teraz” dla istniejącego checka | MVP |
| Asercje HTTP: kod, słowo kluczowe (jest lub go nie ma), ścieżka JSON, nagłówek | MVP |
| **Kategoria błędu:** „cel nie odpowiada” albo „lokalizacja nie może wyjść do sieci” (brak trasy, niedziałający lokalny DNS, odmowa przez lokalny firewall). Błąd lokalizacji nigdy nie daje `DOWN` celu | MVP |
| **„Do sprawdzenia konfiguracji”:** check bez ani jednego udanego wyniku ma `UNKNOWN` z tym oznaczeniem, nie wysyła alertów i podpowiada, co sprawdzić (adres, firewall, lokalna lista agenta, DNS, weryfikacja) | MVP |
| **Próg wolnej odpowiedzi:** przekroczenie (po ponowieniu) daje `DEGRADED`. Próg per check, domyślny z przestrzeni lub instancji, wyłączany | MVP |
| **Flapping:** więcej niż N zmian stanu w oknie daje oznaczenie „niestabilny” i jeden alert. Kolejne zmiany są wyciszone do uspokojenia. Progi konfigurowalne | MVP |
| Pauza, wznowienie, klonowanie, usuwanie, także masowo | MVP |
| Edycja masowa (interwał, tagi, polityka, próg) dla zaznaczonych | MVP |
| Grupy i tagi, filtrowanie po nich | MVP |
| Heartbeat: adres do wywołania, start, sukces i błąd, harmonogram cron lub okres w strefie przestrzeni, grace, kod wyjścia, wiek ostatniego sukcesu | MVP |
| **Gotowe snippety heartbeatu z ograniczonymi ponowieniami i timeoutem**, żeby skrypty użytkowników nie zrobiły zalewu | MVP |
| **Heartbeat odporny na naszą awarię:** pingi przyjmowane i buforowane z oryginalnym czasem także przy niedostępnej bazie. Gdy kontrola wejścia wykryje problem, checki heartbeat mają `UNKNOWN` zamiast `DOWN` | MVP |
| **Limit na identyfikator heartbeatu:** zalew pingów z jednego źródła dostaje natychmiast 429 z czasem ponowienia i nie wpływa na innych. Właściciel checka widzi to w szczegółach | MVP |
| Certyfikat: dni do wygaśnięcia, łańcuch, własne CA, alert o braku odnowienia | MVP |
| **Porzucone checki** (chmura): check `DOWN` od X dni albo przestrzeń nieaktywna od X dni dostaje ostrzeżenie, a potem automatyczną pauzę z powodem. Wznowienie jednym kliknięciem | MVP |
| Domain expiry | v1 |
| Auto-tworzenie checka przy pierwszym wywołaniu nieznanego heartbeatu | v1 |
| Zależności między checkami (np. nie alarmuj o usługach, gdy padł router) | później |

---

## 2. Agent

Szczegóły w planie, sekcja 5. Zasada naczelna: **agent ma jak najmniejsze pole manewru.**
Implementacja: **Go** (statyczna binarka). MVP: **Linux + Docker**, aktualizacje ręczne (bez auto-update).

| Funkcja | Etap |
|---|---|
| Rejestracja jednorazowym tokenem z gotowym poleceniem instalacji | MVP |
| Checki sieciowe z centralnej konfiguracji | MVP |
| **Lokalna lista dozwolonych celów** (sieci, hosty) w pliku agenta. Bez niej agent nie sprawdza niczego z centralnej konfiguracji. Lista jest zgodą administratora sieci, więc checki przez agenta nie wymagają weryfikacji celu | MVP |
| **Własne cele kontrolne** w pliku agenta (np. brama, lokalny DNS), które pomagają odróżnić awarię sieci agenta od awarii celów | MVP |
| Checki lokalne (polecenia) wyłącznie w pliku agenta | MVP |
| Domyślnie bez odsyłania treści odpowiedzi, tylko status, czasy i błąd z kategorią. Treść tylko, gdy agent ma to włączone lokalnie | MVP |
| Wyniki natychmiast, bufor przy braku łączności, opróżnianie w tempie, na które pozwala serwer | MVP |
| **Ponowne łączenie według wspólnej polityki:** szybkie próby z rosnącym odstępem i rozrzutem, potem stan „uśpiony” z próbą co 5 minut. Przestrzega 429 z czasem ponowienia | MVP |
| **Wzorzec zbiorowego błędu:** gdy w tym samym oknie pada ponad X% checków agenta (domyślnie 50%), a inne lokalizacje tego nie widzą albo agent jest jedyną lokalizacją, jego checki dostają `UNKNOWN`, a właściciel jeden alert „agent prawdopodobnie ma problem z siecią” | MVP |
| **Nowy agent i nowe checki zaczynają od „do sprawdzenia konfiguracji”**, dopóki nie przyjdzie pierwszy udany wynik | MVP |
| Monitoring agenta: offline (domyślnie 3 × najkrótszy interwał, nie mniej niż 2 min) i opóźnienie wykonania | MVP |
| Diagnostyka w UI: wersja, ostatni kontakt, ostatni błąd, stan łączenia (połączony / ponawia / uśpiony), bufor, cele odrzucone przez lokalną listę, odsetek błędów z kategorią „lokalizacja” | MVP |
| **Pełna funkcjonalność:** checki z agenta mają te same wykresy, uptime, alerty i miejsce na status page co checki z serwera | MVP |
| Systemy: Linux (binarka) i Docker w MVP; Windows i macOS później | MVP |
| Powiadomienie o nowej wersji, podpisane wydania, aktualizacja ręczna (bez auto-update) | MVP |
| Działa bez uprawnień administratora, bez otwartych portów i bez powłoki dla serwera | MVP |
| mTLS | v1 |

**Dlaczego lokalna lista celów:** bez niej przejęty serwer mógłby kazać agentowi skanować cały intranet i odsyłać treść odpowiedzi, czyli zrobić z agenta furtkę do sieci firmy. Z listą agent sprawdza tylko to, na co zgodził się administrator tej sieci. Check spoza listy jest odrzucany lokalnie, a w UI widać powód.

---

## 2a. Weryfikacja własności celów

Chmura: domyślnie **wymagana** dla każdego celu. Self-host: domyślnie wyłączona.

| Funkcja | Etap |
|---|---|
| **Domena:** rekord DNS TXT z tokenem albo adres z tokenem na domenie, który odpytujemy (domyślnie plik pod `/.well-known/`). Obejmuje subdomeny | MVP |
| **Adres IP, wywołanie z tego IP:** jednorazowy adres z tokenem, który użytkownik wywołuje z maszyny pod tym IP. UI daje gotowe polecenie `curl` osobno dla IPv4 i IPv6. Liczy się adres źródłowy połączenia. Działa dla hostów z samym pingiem albo TCP | MVP |
| **Adres IP, adres z tokenem:** użytkownik wskazuje port i ścieżkę na tym IP (domyślnie `/.well-known/`), a my odpytujemy go jednym zapytaniem, bez przekierowań. Przy HTTPS certyfikat nie jest sprawdzany, bo dowodem jest token | MVP |
| **Email:** link potwierdzający (ten sam mechanizm co potwierdzenie odbiorcy) | MVP |
| Instrukcja krok po kroku z gotowym rekordem, plikiem albo poleceniem do skopiowania i przyciskiem „Sprawdź teraz”. Przy IP UI podpowiada metodę: wywołanie dla pingu i TCP, adres z tokenem dla HTTP | MVP |
| **Token** związany z przestrzenią i jednym celem, ważny krótko (domyślnie 15 minut), z limitem prób | MVP |
| **Ponowna weryfikacja okresowo** (domyślnie co 30 dni). DNS i adres z tokenem sprawdzamy sami. Przy wywołaniu z IP UI daje stały adres ponownej weryfikacji do wpisania w cron (działa tylko z tego IP, więc jego wyciek nic nie daje) i przypomina przed terminem. Brak dowodu daje ostrzeżenie, a po okresie łaski pauzę checków tego celu z powodem | MVP |
| **Tryby** (instancja, z nadpisaniem dla planu albo wybranej przestrzeni): wymagana / niezweryfikowane z ograniczeniami / wyłączona | MVP |
| Tryb „z ograniczeniami”: dłuższy minimalny interwał, limit takich checków w planie, globalny limit na cel. Weryfikacja zdejmuje ograniczenia | MVP |
| Host bez wychodzącego HTTPS i bez serwera HTTP (np. przełącznik, drukarka): tylko przez agenta albo w trybie z ograniczeniami | MVP |
| Ponowienia weryfikacji w granicach wspólnej polityki i limity żądań | MVP |
| Ta sama weryfikacja domeny dla własnej domeny status page | v1 |

**Świadome ograniczenie:** za jednym publicznym IP może stać wiele osób (NAT operatora, sieć firmowa, VPN). Każda z nich przejdzie weryfikację wywołaniem. Prawdziwego właściciela adresu chroni globalny limit żądań na cel, minimalny interwał i strona o nadużyciach.

---

## 3. Alerty i powiadomienia

### Kanały
| Funkcja | Etap |
|---|---|
| Email (zewnętrzny dostawca albo dowolny SMTP), webhook z szablonem, Pushover | MVP |
| „Wyślij test” przy każdym kanale | MVP |
| Potwierdzenie odbiorcy (w chmurze obowiązkowe) | MVP |
| Szablony treści z podglądem (zmienne i bezpieczne formatowanie, bez HTML) | MVP |
| **Stan zdrowia kanału:** po N nieudanych dostawach z rzędu kanał jest „martwy”, a właściciel dostaje alert kanałem zapasowym. Widoczne w Przeglądzie i Ustawieniach | MVP |
| Kanały spoza UE w chmurze (np. Pushover): zasady po konsultacji prawnej | otwarte |
| ntfy, Telegram, Slack, Discord, Teams | v1 |
| SMS, telefon | później |

### Polityki alertów
Polityka określa **kogo**, **kiedy** i **jak** powiadomić. Przestrzeń ma politykę domyślną, a check może wskazać inną.

| Funkcja | Etap |
|---|---|
| Zdarzenia: `DOWN`, powrót do `UP`, opcjonalnie `DEGRADED`, niestabilny, wygasający certyfikat, agent offline, agent z problemem sieci, martwy kanał, wygasająca weryfikacja celu | MVP |
| Opóźnienie przed wysłaniem (np. powiadom, jeśli `DOWN` trwa dłużej niż X) | MVP |
| Powtarzanie do potwierdzenia, z interwałem i **maksymalną liczbą powtórzeń** | MVP |
| **Potwierdzenie (ack)** z linku w powiadomieniu i w UI. Zatrzymuje powtórki | MVP |
| **Wyciszenie checka na określony czas** (snooze) | MVP |
| **Masowe wyciszenie:** jednym ruchem wszystkich albo wybranych (tag, grupa, zaznaczenie) na określony czas, np. podczas globalnej awarii dużego dostawcy. Działa z telefonu | MVP |
| **Alert zbiorczy:** awarie w krótkim oknie (domyślnie 60 s, konfigurowalne) dają jedno powiadomienie z listą. Jedno potwierdzenie obejmuje całą grupę. Powroty do `UP` też są zbierane | MVP |
| **Ograniczone ponowienia dostarczenia:** kilka prób z rosnącym odstępem w budżecie czasu, potem kanał zapasowy, a na końcu stan „nie dostarczono” w logu | MVP |
| **Limit powiadomień w czasie** (część planu). Po przekroczeniu: jedno zbiorcze powiadomienie „limit osiągnięty” i wpis w UI | MVP |
| **Meta-alert dla właściciela instancji:** nagły skok liczby alertów względem normalnego poziomu to sygnał, że problem może leżeć po naszej stronie (np. złe wdrożenie) | MVP |
| Ciche godziny (tylko krytyczne w nocy) | v1 |
| Eskalacje (brak potwierdzenia przez X → następna osoba) | v1 |
| Baner wspólnej przyczyny: wiele checków za tym samym dostawcą pada naraz | v1 |
| Grafik dyżurów | później |

### Log dostarczeń
Każde powiadomienie widać w UI: kanał, próby, odpowiedź odbiorcy, użycie zapasu, stan końcowy. Filtrowanie po checku, kanale i statusie. **MVP.**

---

## 4. Incydenty i planned work

### Incydenty
| Funkcja | Etap |
|---|---|
| Tytuł, status (badanie → zidentyfikowano → monitorowanie → rozwiązano), dotknięte komponenty i wpływ | MVP |
| **Oś z hierarchią:** aktualizacje ludzi jako wyraźne karty, zdarzenia systemowe zwinięte do jednej linii i zapisane zdaniami („Jan oznaczył jako rozwiązany”), najnowsze na górze, pole nowej aktualizacji na górze | MVP |
| Post-mortem jako tekst z bezpiecznym formatowaniem (nagłówki, listy, linki, kod), bez surowego HTML, dołączony do incydentu | MVP |
| Incydent wewnętrzny albo publiczny na status page | MVP |
| **Incydent automatyczny przy `DOWN` komponentu** (szczegóły niżej) | MVP |
| Ukrycie incydentu z publicznej strony. Zostaje w audycie | MVP |
| **Oznaczenie jako fałszywy alarm:** pojedynczo albo masowo dla okresu (np. „wszystkie incydenty między 13:00 a 14:30”). Fałszywe alarmy nie liczą się do uptime ani SLA i są ukryte na status page. Ślad zostaje w audycie | MVP |
| Szablony aktualizacji | v1 |
| Powiadomienie subskrybentów o aktualizacjach | v1 |

**Incydent automatyczny:**
- Powstaje, gdy komponent przejdzie w `DOWN`, czyli po kworum i ponowieniach, nie po pojedynczym błędzie.
- Nigdy nie powstaje, gdy:
  - przyczyna leży po naszej stronie albo po stronie lokalizacji (stan `UNKNOWN`);
  - check ma oznaczenie „do sprawdzenia konfiguracji”.
- **Tryb** ustawiany na poziomie przestrzeni, z nadpisaniem per komponent:
  - **publikuj automatycznie** (domyślnie),
  - **tylko szkic**: incydent czeka na publikację przez człowieka.
- **Warunek publikacji**, konfigurowalny. Domyślnie: od razu po potwierdzonym `DOWN` (opóźnienie 0). Można ustawić próg, np. publikuj, jeśli `DOWN` trwa dłużej niż X albo jeśli dotyczy co najmniej N komponentów.
- Tytuł i pierwsza aktualizacja z szablonu (np. „Problem z: {komponent}”). Człowiek może je zmienić w każdej chwili.
- Przy powrocie do `UP` system dodaje aktualizację „monitorowanie”. Zamknięcie jako „rozwiązano” może być automatyczne albo ręczne, żeby był czas na post-mortem (konfigurowalne).
- Kilka komponentów padających naraz trafia do **jednego** incydentu, tak samo jak przy alercie zbiorczym.
- Komponent oznaczony jako „niestabilny” nie otwiera i nie zamyka incydentu przy każdej zmianie stanu.

### Planned work
| Funkcja | Etap |
|---|---|
| Okno: start, koniec, strefa czasowa, komponenty, opis | MVP |
| Jednorazowe i cykliczne, poprawne przy zmianie czasu | MVP |
| Wycisza alerty i automatyczne incydenty, ale **monitoring działa dalej**, a wyniki są oznaczone | MVP |
| Flaga „wlicz do SLA” | MVP |
| Widoczne na status page z wyprzedzeniem, z automatycznym startem i końcem | MVP |
| Ta sama oś aktualizacji co w incydentach (jeden komponent) | MVP |
| Severity okna | v1 |

---

## 5. Status page

Część tego samego frontendu co panel. Dane ze snapshotu w pamięci API, z kopią we frontendzie na wypadek awarii API.

| Funkcja | Etap |
|---|---|
| Komponenty powiązane z checkami, stan wyliczany automatycznie | MVP |
| Ręczne nadpisanie stanu komponentu | MVP |
| Grupy komponentów | MVP |
| Pasek historii (konfigurowalna długość, domyślnie 90 dni, w granicach planu), bez fałszywych alarmów | MVP |
| Aktualne i przeszłe incydenty, planned work | MVP |
| Wiek danych i czas ostatniej aktualizacji | MVP |
| **Wygląd: logo i kolory** (z kontrolą kontrastu), tryb ciemny, PL/EN. Bez własnego HTML, CSS i skryptów | MVP |
| Wygląd komponentu w stanie `UNKNOWN` z naszej winy | według researchu UI/UX |
| Publiczny JSON ze stanem. To ten sam snapshot co strona, więc koszt jest zerowy | MVP |
| **Przycisk „zgłoś nadużycie”** w stopce (chmura) | MVP |
| **Zawieszona strona** pokazuje neutralny komunikat zamiast treści (chmura) | MVP |
| Kanał RSS/Atom incydentów | v1 |
| Własna domena (z weryfikacją domeny) | v1 |
| Prywatna status page (hasło lub SSO) | v1 |
| Subskrypcje email i webhook z weryfikacją i filtrem komponentów | v1 |
| Badge'e do README | v1 |
| Zagnieżdżone grupy | v1 |
| Awaryjna aktualizacja strony poza panelem | później |

---

## 6. Przegląd (dashboard)
| Funkcja | Etap |
|---|---|
| Na górze to, co jest zepsute: `DOWN`, potem `DEGRADED`, potem `UNKNOWN`, z czasem trwania i oznaczeniem „niestabilny” | MVP |
| **Baner naszej awarii:** „problem z wejściem heartbeatów / lokalizacją / agentem / bazą, statusy wstrzymane” zamiast czerwonej listy | MVP |
| Niepotwierdzone alerty z przyciskiem ack | MVP |
| Przycisk masowego wyciszenia | MVP |
| Liczniki stanów, które po kliknięciu filtrują listę | MVP |
| Feed zdarzeń na żywo | MVP |
| Zdrowie lokalizacji, wejścia heartbeatów i agentów, z opóźnieniem wykonania i stanem łączenia | MVP |
| Checki „do sprawdzenia konfiguracji” i cele z wygasającą weryfikacją | MVP |
| Martwe kanały powiadomień i niedostarczone powiadomienia | MVP |
| Trwające i nadchodzące planned work | MVP |
| Zużycie planu, gdy blisko limitu (także limitu powiadomień) | MVP |
| Baner „konfiguracja różni się od pliku” (sekcja 9) | MVP |

---

## 7. Szczegóły checka
| Funkcja | Etap |
|---|---|
| Aktualny stan z informacją, z ilu lokalizacji pochodzi decyzja, i powodem `UNKNOWN`, jeśli dotyczy | MVP |
| Wykres czasu odpowiedzi w fazach (DNS, połączenie, TLS, pierwszy bajt) per lokalizacja, z linią progu wolnej odpowiedzi | MVP |
| Zakres 24 h, 7, 30, 60, 365 dni (w granicach planu), w URL | MVP |
| Uptime w wybranym okresie, bez fałszywych alarmów | MVP |
| Lista zdarzeń i błędów z kategorią, pełnym requestem i odpowiedzią, stronicowana | MVP |
| Podpowiedzi dla stanu „do sprawdzenia konfiguracji” | MVP |
| Stan weryfikacji celu, metoda i data następnego sprawdzenia | MVP |
| Certyfikat: wystawca, łańcuch, daty | MVP |
| Historia powiadomień dla tego checka, ze stanami końcowymi | MVP |
| Historia zmian konfiguracji checka, z informacją, czy zmiana przyszła z UI, API czy pliku | MVP |
| Dla heartbeatu: odrzucone pingi (limit) i pingi zapisane z bufora | MVP |

---

## 8. Użytkownicy, role, dostęp
| Funkcja | Etap |
|---|---|
| Role: **Admin** (wszystko), **Edytor** (checki, progi, incydenty, status pages, weryfikacja celów), **Podgląd** (tylko odczyt) | MVP |
| Zaproszenia mailem | MVP |
| **Self-host:** lokalny admin i SSO przez dowolnego dostawcę OIDC | MVP |
| **Chmura:** logowanie przez GitHub, Google albo jednorazowy link w emailu (krótka ważność, limit żądań) | MVP |
| **Własne OIDC przestrzeni** (chmura): firma podpina własne SSO do swojej przestrzeni | MVP |
| 2FA dla kont lokalnych | MVP |
| Tokeny API z zakresem (odczyt lub zapis) i datą ważności | MVP |
| Lista aktywnych sesji i wylogowanie wszędzie | MVP |
| Mapowanie grup z SSO na role | v1 |
| SAML, SCIM | później |

---

## 9. API, plik konfiguracyjny i różnice

**Zasada: UI i plik są równorzędne.** Wszystko można edytować w UI i wszystko można trzymać w pliku. System zawsze pokazuje, czy jedno różni się od drugiego.

| Funkcja | Etap |
|---|---|
| Pełne API z dokumentacją generowaną z kodu, stronicowaniem i filtrowaniem | MVP |
| Odpowiedzi 429 z czasem ponowienia, opisane w dokumentacji | MVP |
| Plik deklaratywny: checki, kanały, polityki, komponenty, status pages | MVP |
| **Wersja schematu w każdym pliku.** Plik ze starszej wersji jest migrowany automatycznie, z podglądem zmian. Plik z nowszej, nieznanej wersji jest odrzucany z jasnym komunikatem | MVP |
| **Edycja w UI wszystkiego**, także obiektów, które przyszły z pliku | MVP |
| **Eksport bieżącej konfiguracji do pliku** (całość albo wybrane obiekty), np. żeby zatwierdzić zmiany z UI w repozytorium | MVP |
| **Wykrywanie różnic:** system pamięta ostatnio wgrany plik i porównuje go z bieżącym stanem | MVP |
| **Informacja w UI:** baner „N obiektów różni się od pliku” z listą różnic, przyciskiem eksportu i znacznikiem na zmienionych obiektach | MVP |
| **Podgląd różnic przed wgraniem pliku:** co zostanie dodane, zmienione, usunięte, a osobno, które zmiany z UI zostaną nadpisane | MVP |
| **Informacja przy wdrożeniu:** jeśli plik przy starcie różni się od stanu z UI, różnica trafia do logu, do UI i jako alert do właściciela | MVP |
| **Zachowanie przy wdrożeniu** (konfigurowalne, niżej) | MVP |
| Plik nie omija weryfikacji celów: check na niezweryfikowany cel z pliku jest odrzucany z jasnym komunikatem | MVP |
| Narzędzia infrastructure-as-code | v1 |
| Importer z Uptime Kuma | v1 |

**Zachowanie przy wdrożeniu:**
- **Domyślnie:**
  - zmiany z pliku, które nie dotykają obiektów zmienionych w UI, wgrywają się same;
  - obiekty zmienione i w pliku, i w UI to konflikt. System ich **nie nadpisuje**, zgłasza różnicę (log, UI, alert) i czeka na decyzję: „wgraj plik” albo „eksportuj UI do pliku”.
- **Tryb „plik zawsze wygrywa”** do włączenia w konfiguracji instancji, dla tych, którzy trzymają całą konfigurację w gicie. Nadpisane zmiany z UI i tak trafiają do audytu.

---

## 10. Raporty
| Funkcja | Etap |
|---|---|
| Uptime per check, grupa i komponent za wybrany okres, bez fałszywych alarmów | MVP |
| Eksport CSV | MVP |
| SLA w minutach, z uwzględnieniem flagi planned work i fałszywych alarmów | v1 |
| Raport okresowy mailem | v1 |
| Eksport metryk dla zewnętrznych systemów | v1 |

---

## 11. Ustawienia przestrzeni
| Funkcja | Etap |
|---|---|
| Nazwa, strefa czasowa, język | MVP |
| Domyślne wartości checków (w granicach planu), w tym próg wolnej odpowiedzi, flapping i kworum | MVP |
| **Zweryfikowane cele:** domeny, IP i emaile ze statusem, metodą, datą następnego sprawdzenia i instrukcją (przy wywołaniu z IP także adres ponownej weryfikacji do crona) | MVP |
| Tryb incydentów automatycznych i warunek publikacji | MVP |
| Okno alertu zbiorczego | MVP |
| Kanały (ze stanem zdrowia), polityki, log dostarczeń | MVP |
| Lokalizacje i agenci | MVP |
| Użytkownicy, role, tokeny API | MVP |
| **Sposoby logowania** (chmura): podpięcie własnego OIDC przestrzeni | MVP |
| Sekrety | MVP |
| Plan i zużycie limitów, także powiadomień i checków niezweryfikowanych | MVP |
| Konfiguracja: eksport do pliku, różnice względem pliku | MVP |
| Dziennik audytu z filtrami | MVP |
| Eksport całej przestrzeni (z wersją schematu) i usunięcie przestrzeni (GDPR) | MVP |
| Eksport audytu | v1 |

---

## 12. Administracja instancji
| Funkcja | Etap |
|---|---|
| Przestrzenie: lista, zużycie, plan, zawieszenie | MVP |
| Plany: definicje limitów (w tym powiadomień i checków niezweryfikowanych), przypisanie | MVP |
| Rejestracja: tryb (zamknięta, zaproszenia, lista oczekujących), kody, limit kont | MVP |
| **Sposoby logowania:** lokalny admin, OIDC, a w chmurze GitHub, Google, link w emailu i zgoda na własne OIDC przestrzeni | MVP |
| **Tryb weryfikacji celów:** wymagana / z ograniczeniami / wyłączona, z odblokowaniem dla planu albo wybranej przestrzeni, ważność tokenu, okres ponownej weryfikacji, okres łaski, nazwy wejścia dla wywołań z IPv4 i IPv6 | MVP |
| **Zgłoszenia nadużyć status pages:** lista zgłoszeń, zawieszenie i przywrócenie strony jednym kliknięciem, powód widoczny dla właściciela | MVP |
| Lokalizacje i cele kontrolne, próg wzorca zbiorowego błędu, próg opóźnienia wykonania | MVP |
| Kontrola własnego wejścia heartbeatów: stan, częstotliwość, okres rozruchu | MVP |
| Limity na wejściu: na identyfikator, na agenta, na klienta, timeouty, próg alertu połączeń | MVP |
| **Polityka ponowień:** domyślne limity i odstępy dla checków, powiadomień, archiwum, bufora, weryfikacji i ponownego łączenia | MVP |
| Polityka ruchu wychodzącego | MVP |
| Porzucone checki: progi i lista spauzowanych | MVP |
| Zachowanie przy wdrożeniu z różnicą konfiguracji | MVP |
| **Klucz szyfrowania sekretów:** rotacja bez przestoju, data ostatniej kopii klucza | MVP |
| **Zrzuty bazy:** częstotliwość ustawiana przez operatora, retencja | MVP |
| **Stan systemu:** wersja, lider, opóźnienie wykonania, kontrola wejścia, obciążenie połączeń, liczba alertów względem normy, elementy w stanie „uśpiony”, zaległości archiwum, stan watchdoga, ostatni zrzut bazy, ostatni test odtworzenia (z odszyfrowaniem sekretów), zrzut przed ostatnią migracją. Czytelny na telefonie | MVP |
| Strona informacyjna o ruchu: lista IP (także w formacie maszynowym), zapowiedzi zmian adresów, kontakt w sprawie nadużyć | MVP |
| Jakość łącza między VPS | v1 (z drugim VPS) |

---

## 13. Decyzje (podjęte)
1. Wolna odpowiedź daje `DEGRADED`. Próg per check, zmieniany przez każdego edytora.
2. Alert zbiorczy w MVP, z konfigurowalnym oknem.
3. Incydent automatyczny przy `DOWN` komponentu, domyślnie publikowany od razu. Tryb „tylko szkic” i warunek publikacji (czas trwania, liczba komponentów) są konfigurowalne.
4. UI i plik równorzędne: edycja w UI, eksport do pliku, wykrywanie różnic widoczne w UI i przy wdrożeniu.
5. Wdrożenie z różnicą: bez konfliktu wgrywa się samo, a konflikt czeka na decyzję. Tryb „plik zawsze wygrywa” jest w konfiguracji.
6. Okno offline agenta konfigurowalne, domyślnie 3 × najkrótszy interwał, nie mniej niż 2 min.
7. Lekcje z cudzych błędów (flapping, martwy kanał, limit powiadomień, opóźnienie wykonania, stronicowanie) wchodzą w MVP, w etapach według planu (sekcja 14).
8. Lekcje z post-mortemów (kontrola wejścia, okres rozruchu, buforowanie pingów, limity na wejściu, masowe wyciszenie, meta-alert, fałszywe alarmy, porzucone checki, oś z hierarchią, pełny self-host) wchodzą w MVP, w etapach według planu (sekcja 14).
9. **Żadnych nieograniczonych ponowień.** Jedna wspólna polityka. Wyjątek: ponowne łączenie (agent → API, API → baza) przechodzi po limicie w stan „uśpiony” z próbą co 5 minut i alertem.
10. **W chmurze każdy cel musi być zweryfikowany** (domena, IP, email). Tryb „niezweryfikowane z ograniczeniami” można odblokować dla instancji, planu albo przestrzeni. Checki przez agenta nie wymagają weryfikacji.
11. **Agent albo lokalizacja za firewallem nie daje fali `DOWN`:** kategoria błędu, wzorzec zbiorowego błędu, „do sprawdzenia konfiguracji”.
12. **Adres IP weryfikowany dwiema metodami:** wywołanie z weryfikowanego IP albo adres z tokenem, który odpytujemy. Współdzielony adres (NAT, VPN) to świadome ograniczenie, osłonięte globalnym limitem na cel.
13. **Kworum:** przy 1–2 lokalizacjach „wszystkie”, przy więcej niż 2 „większość”. Konfigurowalne per check.
14. **Bez własnego HTML, CSS i skryptów** nigdzie. Status page: logo i kolory. Zgłaszanie nadużyć i zawieszanie stron w chmurze.
15. **Logowanie:** self-host dowolne OIDC i lokalny admin; chmura GitHub, Google, link w emailu, opcjonalnie własne OIDC przestrzeni.
16. **Tylko część wspólna bazy produkcyjnej i wbudowanej**, blokada lidera jako tabela, CI na obu bazach.
17. **Wersja schematu** w pliku konfiguracyjnym i eksporcie przestrzeni, z automatyczną migracją starszej wersji.
18. **Klucz szyfrowania sekretów** z osobną kopią poza VPS i rotacją. Test odtworzenia sprawdza odszyfrowanie.
19. Częstotliwość zrzutu bazy ustala operator.
20. **Stack:** Java 25 LTS, Spring Boot 4.x, Maven, Spring Data JDBC + `JdbcClient`, Liquibase XML, PostgreSQL/HSQLDB (plik, jeden proces). **Agent:** Go, Linux + Docker w MVP, aktualizacje ręczne. Szczegóły: `decisions/0001-stack.md`.

## 14. Otwarte
1. Kanały powiadomień spoza UE w chmurze: po konsultacji prawnej.
2. Wygląd `UNKNOWN` z naszej winy na status page: osobny research UI/UX (brief: `research/ui-ux-brief.md`).
