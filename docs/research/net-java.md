# Research: pomiary sieciowe w Javie 25 dla serwera wakey

## Zakres

Badam, jak serwer wakey (Java 25 LTS, Spring Boot 4.x, wątki wirtualne, bez WebFlux) ma wykonywać pomiary sieciowe z backlogu 2a: czasy faz HTTP, własne DNS z przypięciem adresu i ochroną przed DNS rebinding, ICMP na serwerze, certyfikaty, zachowanie wątków wirtualnych i kategorię błędu. Porównuję „samą platformę” (JDK 25), własny mały kod i biblioteki (OkHttp, Apache HttpClient 5, Netty, dnsjava, icmp4j). Poza zakresem są: implementacja agenta Go (backlog 2b), UI, baza, alerty i decyzja o przyjęciu rekomendacji. To research: pokazuję kompromisy i pytania, decyzję podejmuje właściciel po grill-me.

## Opcje

### 1. Czasy faz HTTP i klient HTTP/1.1

Potrzebne fazy: DNS, połączenie TCP, handshake TLS, pierwszy bajt odpowiedzi i całość. Wykres faz z sekcji 7 `product-features.md` wymaga ich per lokalizacja, także przy 5 000 checków.

**Platforma: `java.net.http.HttpClient` (JDK 25).** Publiczne API daje `send`/`sendAsync`, `connectTimeout`, `HttpRequest.timeout`, `followRedirects`, `SSLContext`, `SSLParameters` i `HttpResponse.sslSession()`; w JDK 25 doszedł `connectionLabel()` [1][2][3]. Linki API: [HttpClient](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.html), [HttpRequest](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpRequest.html), [HttpResponse](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpResponse.html). Nie ma jednak żadnego event listenera ani metod faz: `HttpRequest` ma tylko timeout, a `HttpResponse` status, nagłówki, ciało, sesję TLS i wersję [2][3]. Całkowity czas można zmierzyć `System.nanoTime()` wokół `send`, ale DNS, TCP, TLS i pierwszy bajt nie są rozdzielone. Nie ma też publicznego API, by dla jednego żądania wskazać rozwiązany adres IP (resolucja jest systemowa, przez SPI JVM) [4][5]. Domyślnie klient preferuje HTTP/2 [1]. Wniosek: sam JDK nie wystarcza do wykresu faz ani do przypięcia adresu; nadaje się jako awaryjna ścieżka dla celów „tylko h2”.

**Własny kod: minimalny klient HTTP/1.1 na `Socket`/`SSLSocket`.** `Socket` daje TCP, `SSLSocket` dokłada TLS, a `SSLParameters` pozwala ustawić SNI, ALPN i `setEndpointIdentificationAlgorithm("HTTPS")` [13][14][15]. Czas każdej fazy bierzemy `System.nanoTime()`: rozwiązanie DNS, `connect`, `startHandshake`, zapis nagłówków, pierwszy odczyt bajtu, koniec ciała. Trzeba obsłużyć: GET/HEAD/POST, nagłówki, `Content-Length`, `Transfer-Encoding: chunked`, limit treści, limit nagłówków, przekierowania z limitem i bez zmiany hosta na niezweryfikowany, timeouty i IPv4/IPv6 [20][21]. To ok. 800–1200 linii i 8–12 dni jednej osoby; najtrudniejsze są parser odpowiedzi, obsługa błędów połączenia i testy na złych serwerach.

**Biblioteki z hookami zdarzeń.**
- **OkHttp 5.x** ma `EventListener` z dokładnie potrzebnymi zdarzeniami: `callStart`, `dnsStart/dnsEnd`, `connectStart/connectEnd`, `secureConnectStart/secureConnectEnd`, `responseHeadersStart/End`, `responseBodyStart/End`, `callEnd` [24][25]. Pozwala też wymienić `Dns` na własny (przypięcie adresu), wyłączyć przekierowania i połączyć się bez poolingu, żeby fazy nie były ukryte przez reuse. Licencja Apache-2.0; 5 artefaktów runtime: `okhttp`, `okio`, `okio-jvm`, `kotlin-stdlib`, `org.jetbrains:annotations` [31]. Koszt: Kotlin w projekcie Java i własna konfiguracja poolingu.
- **Apache HttpClient 5.6** ma dojrzały connection manager, TLS i przekierowania, ale rdzeń nie ma odpowiednika `EventListener` per żądanie; fazy trzeba zbierać własnymi dekoratorami connection managera/TLS albo metrykami obserwowalności (Micrometer), które są agregatami, nie zdarzeniami konkretnego checka [26][27]. Licencja Apache-2.0; 3 zależności runtime: `httpcore5`, `httpcore5-h2`, `slf4j-api` [32].
- **Netty 4.1** daje kodek HTTP i pipeline, ale nie gotowy zestaw faz; trzeba pisać handlery i mierzyć w pipeline [28]. Licencja Apache-2.0; 8 artefaktów runtime (`netty-codec-http` + 7 przechodnich, m.in. `netty-handler`, `netty-transport`, `netty-resolver`) [33]. To ciężka opcja dla jednego opiekuna.

**HTTP/2 w MVP.** `HttpClient` domyślnie preferuje HTTP/2 [1], ale do monitoringu wystarcza HTTP/1.1: daje status, nagłówki, ciało i czasy, a multiplexing nie jest potrzebny, bo każdy check i tak potrzebuje świeżego połączenia do pomiaru faz. HTTP/2 ma znaczenie tylko dla celów, które odrzucają HTTP/1.1; takich jest mało, a można je obsłużyć awaryjnie `HttpClient` (bez faz) lub OkHttp. Standard HTTP/2 to [RFC 9113](https://www.rfc-editor.org/rfc/rfc9113.html) [22]. Rekomendacja: HTTP/1.1 w MVP, zapisywać negocjowaną wersję i dodać fallback, jeśli pojawią się cele h2-only.

### 2. Własne rozwiązywanie DNS, przypięcie adresu i DNS rebinding

Potrzebne: A/AAAA, wiele adresów, IPv4/IPv6, timeouty, cache/TTL, pomiar czasu DNS, TXT do weryfikacji domeny (sekcja 2a) oraz ochrona przed DNS rebinding i blokada adresów prywatnych, link-local i metadata w chmurze.

**Platforma: `InetAddress` + `InetAddressResolverProvider` (JEP 418).** `InetAddress.getAllByName()` zwraca wszystkie adresy, a `InetSocketAddress(adres, port)` pozwala połączyć się z wybranym IP bez drugiego lookup [4]. JEP 418 daje SPI globalnego resolvera (`InetAddressResolverProvider`), ale jest **jeden na JVM**, nie per check [5][6][7]; zobacz [JEP 418](https://openjdk.org/jeps/418). Cache jest globalny i sterowany właściwościami `networkaddress.cache.ttl`, `networkaddress.cache.negative.ttl` i `networkaddress.cache.stale.ttl`; nie ma per-check TTL ani per-lookup timeoutu — szczegóły w [InetAddress](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/net/InetAddress.html) [4]. `InetAddress` obsługuje tylko operacje potrzebne tej klasie — **nie ma TXT**; do TXT trzeba JNDI DNS (moduł `jdk.naming.dns`) albo biblioteki [7]. Wbudowany resolver na Linuksie korzysta z natywnego resolvera systemowego, więc blokuje nośnik wątku wirtualnego; JDK łagodzi to chwilowym zwiększeniem równoległości, ale to nie skaluje się do 5 000 checków [7][8][9].

**Własny kod: mały klient DNS na `DatagramChannel`/`DatagramSocket`.** A/AAAA/TXT, kompresja nazw, CNAME, TTL, cache z honorowaniem TTL, retry, TCP fallback przy truncation, IPv4/IPv6, timeout per zapytanie. To 400–700 linii i 5–8 dni; najtrudniejsze są parser DNS (kompresja wskaźników), EDNS(0), obsługa błędów i testy ze stubem DNS. Ryzyko: własny parser nieznanego ruchu to klasa błędów bezpieczeństwa; dlatego to opcja tylko przy twardym wymaganiu zero zależności.

**Biblioteki.**
- **dnsjava 3.7.1**: BSD-3-Clause, dojrzały, obsługuje A/AAAA/TXT/CNAME/TTL/cache, DNSSEC i TCP fallback; w wersji 3 resolwery są nieblokujące i nie tworzą wątku na zapytanie — [dnsjava](https://github.com/dnsjava/dnsjava) [29]. POM: 1 runtime przechodnia (`slf4j-api` 1.7.36) plus opcjonalne JNA na Windows [30]. Można jej używać bezpośrednio per check (nie jako globalnego SPI), więc różne polityki DNS nie kolidują.
- **JNDI DNS (JDK)**: platforma, nie biblioteka; pozwala odpytować TXT, ale API jest archaiczne, a TTL i szczegóły cache są słabo dostępne. Dobre jako awaryjna ścieżka zero-dep, nie jako podstawa.
- **Netty `DnsNameResolver`**: nieblokujący resolver z TTL i IPv4/IPv6, ale ciągnie Netty (8 artefaktów) [33]; nieproporcjonalne do jednej potrzeby.

**DNS rebinding i przypięcie.** Rozwiązujemy nazwę **raz**, walidujemy **wszystkie** zwrócone adresy względem polityki (prywatne, loopback, link-local, multicast, metadata 169.254.169.254 i `fd00:ec2::254`, IPv4-mapped IPv6, NAT64) i łączymy się wyłącznie z zatwierdzonym adresem, zachowując oryginalny host do nagłówka `Host`, SNI i weryfikacji certyfikatu [23]. Drugi, niekontrolowany lookup między walidacją a połączeniem pozwala obejść ochronę — dokładnie to opisuje [OWASP SSRF Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) [23]. Przy wielu adresach próbujemy kolejne w budżecie czasu, preferując IPv6/IPv4 zgodnie z polityką; każdy adres walidujemy osobno.

### 3. ICMP ping na serwerze

**Platforma: `InetAddress.isReachable(timeout)`.** Dokumentacja [InetAddress.isReachable](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/net/InetAddress.html) mówi wprost: to best effort; typowo używa ICMP ECHO, jeśli uda się uzyskać uprawnienie, inaczej próbuje TCP na porcie 7 (Echo) [4]. W praktyce port 7 prawie nigdy nie jest otwarty, więc wynik `false` nie oznacza „cel padł”, tylko „nie udało się”. Implementacja jest natywna i nie integruje się z diagnostyką pinningu wątków wirtualnych [19].

**Uprawnienia.** W Linuksie surowy socket ICMP wymaga `CAP_NET_RAW` [17]. Nieuprzywilejowany socket ICMP (datagram) zależy od `net.ipv4.ping_group_range`; domyślnie `1 0`, czyli **żadna grupa** nie może go utworzyć — patrz [icmp(7)](https://man7.org/linux/man-pages/man7/icmp.7.html) [16]. Docker domyślnie zostawia `NET_RAW` [18], ale rootless, hardened runtime, Kubernetes z `drop: [ALL]` albo polityka bezpieczeństwa mogą go odebrać; nie można na tym budować poprawności produktu. Systemowy `ping` może mieć setuid/file capability, ale w kontenerze też wymaga uprawnień i dodaje proces, parsowanie tekstu, zależność od locale i ryzyko injection przy argumentach.

**Własny kod / biblioteki.** Własny ICMP w Javie wymaga JNI/JNA albo procesu `ping`; JDK nie ma surowych gniazd (tylko TCP `Socket`, UDP `DatagramSocket` i multicast) [15]. Biblioteka **icmp4j** istnieje, ale repozytorium jest zarchiwizowane i nieutrzymywane, a licencja w źródłach jest niejednoznaczna (LGPLv3/GPLv3) [34]. Nie rekomenduję jej do open source bez decyzji prawnej.

**Rekomendacja dla potrzeby:** serwer **nie powinien** opierać się na ICMP w MVP. Dla checka TCP i HTTP „czy cel żyje” wystarcza TCP connect do skonfigurowanego portu; dla HTTP i tak łączymy się TCP. ICMP zostaje dla agenta (backlog 2b), gdzie jest lokalna lista celów i kontekst sieci. Jeśli właściciel chce ICMP na serwerze, to jako opcjonalny, jawnie włączany tryb z systemowym `ping` i uczciwym komunikatem „wymaga uprawnień”, nigdy jako domyślny.

### 4. Certyfikaty

**Platforma.** [SSLSession.getPeerCertificates()](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/javax/net/ssl/SSLSession.html) zwraca łańcuch (end-entity pierwszy), ale dokumentacja ostrzega, że **zwrócona wartość może nie być poprawnym łańcuchem i nie służy do decyzji o zaufaniu** [11]. Do walidacji i własnego CA używamy `X509ExtendedTrustManager` + `TrustManagerFactory` + `SSLContext`; `SSLParameters.setEndpointIdentificationAlgorithm("HTTPS")` włącza weryfikację nazwy hosta, a `setServerNames()` ustawia SNI [12][14]. `X509Certificate.getNotAfter()` daje datę wygaśnięcia; dni do wygaśnięcia liczymy w UTC. Przy przypiętym IP łączymy się z adresem, ale `SSLSocketFactory.createSocket(Socket, host, port, true)` przekazuje oryginalny host do SNI i weryfikacji [13][14].

**Własny kod.** ~150–250 linii: pobranie łańcucha, mapowanie wystawca/okres ważności, własne CA per check, cache `SSLContext` per CA, klasyfikacja „wygasa za N dni” i „brak odnowienia” (np. 30/14/7 dni, konfigurowalne). Trudne: łańcuchy niepełne, certyfikaty self-signed, brak SNI, IDN/punycode, różne zaufane magazyny. Testy: lokalny `SSLServerSocket` z certyfikatem self-signed, łańcuchem, wygasłym certyfikatem i złym hostem.

**Biblioteki.** Bouncy Castle (`bcprov`, `bcpkix`) daje pełny parsing i obsługę egzotycznych algorytmów, ale to duża zależność kryptograficzna i nie jest potrzebna, dopóki JDK obsługuje standardowe łańcuchy [35]. OkHttp/Apache HttpClient 5 udostępniają `SSLSession`/`Handshake`, ale pod spodem używają JDK; nie rozwiązują problemu własnego CA lepiej niż `SSLContext`.

### 5. Wątki wirtualne

**Platforma.** JEP 444 wprowadził wątki wirtualne w Javie 21 i mówi, że networking `java.net`/`java.nio.channels` jest przyjazny wątkom wirtualnym: blokujące połączenie lub odczyt zwalnia nośnik [8]. JEP 491 (dostarczony w Javie 24, więc obecny w 25) usuwa prawie wszystkie przypadki pinningu w `synchronized`; pozostają native frames, class initializer i oczekiwanie na inicjalizację klasy — patrz [JEP 491](https://openjdk.org/jeps/491) [9]. Wbudowany resolver DNS jest natywny i może pinować; JDK kompensuje to chwilowym zwiększeniem równoległości, ale JEP 418 wprost wskazuje resolver SPI jako rozwiązanie dla Loom [7]. `InetAddress.isReachable` jest natywne i nie raportuje pinningu w diagnostyce [19].

**Własny kod.** Klient na `Socket`/`SSLSocket` jest przyjazny wątkom wirtualnym, bo `connect`, `read`, `write` i `startHandshake` to blokujące operacje JDK, które oddają nośnik. Trzeba tylko nie trzymać `synchronized` wokół I/O i nie blokować w `Object.wait()`; jeśli potrzebny lock, `ReentrantLock` [9]. Unikać `InetAddress.isReachable` i wbudowanego DNS na ścieżce gorącej; używać dnsjava (nieblokujące) albo własnego resolvera na `DatagramChannel`.

**Biblioteki.** dnsjava 3 deklaruje nieblokujące resolwery [29]. OkHttp i Apache HttpClient 5 działają na blokujących gniazdach JDK, więc ich I/O jest zasadniczo VT-friendly, ale mają własne pule wątków i blokady — trzeba to potwierdzić testem pinningu (JFR `jdk.VirtualThreadPinned`) [8]. Netty opiera się na event loopach, nie na wątkach wirtualnych; nie jest naturalnym wyborem dla executora na VT.

### 6. Kategoria błędu

**Platforma: mapowanie wyjątków JDK.** Potrzebne są dwie kategorie produktu: „cel nie odpowiada” i „lokalizacja nie może wyjść do sieci”. Surowe wyjątki nie mówią tego wprost; trzeba je sklasyfikować i wesprzeć celami kontrolnymi.

| Sygnał | Typowa przyczyna | Kategoria wstępna | Uwaga |
|---|---|---|---|
| `UnknownHostException` | brak rekordu DNS albo awaria lokalnego resolvera | zależy | Cel kontrolny DNS rozstrzyga: jeśli kontrolny też nie rozwiązuje, to lokalizacja; jeśli tylko cel, to cel [4] |
| `ConnectException` (refused) | port zamknięty / usługa nie działa | cel | Połączenie TCP doszło do celu |
| `NoRouteToHostException` | brak trasy, firewall lokalny, sieć lokalizacji | lokalizacja | Nie mylić z filtrowaniem po stronie celu; potrzebny cel kontrolny |
| `SocketTimeoutException` (connect/read) | wolny cel albo lokalna sieć/firewall | zależy | Connect timeout do wielu celów naraz sugeruje lokalizację; read timeout to raczej cel |
| `SSLHandshakeException` | zły certyfikat, brak SNI, MITM, zły zegar | cel (lub lokalizacja przy MITM) | Jeśli kontrola TLS też pada, to lokalizacja |
| `BindException` | brak lokalnego portu/adresu | lokalizacja | Problem lokalny |
| `PortUnreachableException` (UDP) | ICMP port unreachable | cel | Dla UDP |
| `HttpTimeoutException` | przekroczony timeout żądania | cel | Po nawiązaniu połączenia |

**Własny kod.** Klasyfikator ~150–250 linii: mapowanie wyjątków, kontekst fazy (DNS/TCP/TLS/HTTP), cele kontrolne (np. znany publiczny endpoint, lokalny gateway/DNS) i reguła „jeśli kontrola padła, obniż do lokalizacji”. Do tego wzorzec zbiorczego błędu z sekcji 2 produktu: jeśli w oknie pada >X% checków jednej lokalizacji, a inne lokalizacje widzą cel, to problem lokalizacji. To wymaga danych z wielu checków, nie tylko jednego wyjątku.

**Biblioteki.** Nie rozwiązują klasyfikacji: OkHttp i HttpClient 5 mają własne hierarchie `IOException`, ale nie znają pojęcia „lokalizacja nie może wyjść”. Trzeba dopisać to samo mapowanie; biblioteka może co najwyżej dać lepszy kontekst (np. `connectFailed` w OkHttp) [24].

## Porównanie

### Klient HTTP

| Kryterium | JDK `HttpClient` | Własny HTTP/1.1 | OkHttp 5 | HttpClient 5.6 | Netty 4.1 |
|---|---|---|---|---|---|
| Czasy faz per żądanie | nie | tak, pełna kontrola | tak (`EventListener`) [24] | nie w rdzeniu; metryki agregatowe [27] | nie; trzeba pisać handlery [28] |
| Przypięcie IP per check | nie | tak | tak przez własny `Dns` | tak przez custom resolver/connection manager | tak przez custom resolver |
| Przekierowania bez zmiany hosta | `NEVER`/`NORMAL` [1] | własny limit | `followRedirects(false)` | własna polityka | własna obsługa |
| Limit treści i nagłówków | przez `BodyHandler` | własny, twardy | własny odczyt | własny odczyt | własny kodek |
| HTTP/2 | tak, domyślnie [1] | nie (MVP) | tak | tak (`httpcore5-h2`) | tak |
| Wątki wirtualne | tak dla I/O; DNS może pinować [8] | tak | tak dla sync; własne pule | tak dla classic | event loop, nie VT |
| Zależności runtime | 0 | 0 | 5 artefaktów [31] | 3 przechodnie [32] | 8 artefaktów [33] |
| Licencja | JDK | — | Apache-2.0 | Apache-2.0 | Apache-2.0 |
| Koszt utrzymania | mały | 800–1200 LOC, 8–12 dni | mały/średni, Kotlin | średni | duży |

### DNS

| Kryterium | `InetAddress` / SPI | Własny DNS | dnsjava 3.7.1 | JNDI DNS | Netty resolver |
|---|---|---|---|---|---|
| A/AAAA | tak [4] | tak | tak | tak | tak |
| TXT (weryfikacja domeny) | nie [7] | tak | tak [29] | tak | tak |
| TTL i cache per check | nie; globalne właściwości [4] | tak | tak | ograniczone | tak |
| Timeout per zapytanie | nie | tak | tak | ograniczone | tak |
| Przypięcie jednego adresu | tak (po `getAllByName`) | tak | tak | tak | tak |
| Wiele adresów / IPv6 | tak | tak | tak | tak | tak |
| VT bez pinningu | nie (natywny resolver) [7] | tak | tak (v3 non-blocking) [29] | częściowo | tak |
| Zależności | 0 | 0 | 1 (`slf4j-api`) [30] | 0 (moduł JDK) | Netty (8) [33] |
| Ryzyko | cache/TTL, brak TXT | parser DNS | małe | API, TTL | duże |

### ICMP, certyfikaty, błędy

| Potrzeba | Platforma | Własny kod | Biblioteka | Wniosek |
|---|---|---|---|---|
| ICMP | `isReachable` (best effort, TCP 7) [4] | proces `ping` lub JNI/JNA | icmp4j (archived, LGPL/GPL) [34] | nie używać na serwerze w MVP; TCP connect |
| Certyfikaty | `SSLSession`, `X509ExtendedTrustManager`, `SSLContext` [11][12][14] | inspekcja + wygaśnięcie, ~200 LOC | Bouncy Castle [35] | JDK wystarcza |
| Kategoria błędu | wyjątki JDK [4] | klasyfikator + cele kontrolne | brak gotowego mapowania | własny klasyfikator |

## Własna implementacja

| Element | Szacunek | Trudne miejsca | Testy |
|---|---|---|---|
| Klient HTTP/1.1 na `Socket`/`SSLSocket` | 800–1200 LOC, 8–12 dni | parser statusu/nagłówków, chunked, `Content-Length`, HEAD bez ciała, przekierowania z limitem, limit treści, TLS/SNI, timeouty, IPv4/IPv6 | lokalny `ServerSocket`/`SSLServerSocket`: normalna odpowiedź, chunked, złe nagłówki, slowloris, redirect loop, za duże ciało, zamknięcie w połowie, IPv6, certyfikat self-signed |
| Klasyfikator błędów + cele kontrolne | 150–250 LOC, 2–3 dni | odróżnienie DNS celu od DNS lokalizacji, timeout connect vs read, wiele celów | stub DNS, zamknięty port, brak trasy, zły certyfikat, kontrolny endpoint padający |
| Certyfikaty i wygaśnięcie | 150–250 LOC, 2–3 dni | łańcuchy niepełne, self-signed, własne CA, SNI przy IP | lokalny TLS z łańcuchem, wygasły certyfikat, zły host, brak SNI |
| Własny DNS (tylko jeśli zero-dep) | 400–700 LOC, 5–8 dni | kompresja nazw, CNAME, TTL, TCP fallback, EDNS, cache | stub DNS UDP/TCP, truncation, złe pakiety, wiele adresów |
| ICMP | nie pisać | surowe gniazda, uprawnienia, parsowanie `ping` | — |

Razem własny wariant bez biblioteki DNS: ok. 1100–1700 LOC i 12–18 dni jednej osoby, plus testy i utrzymanie parsera. Z dnsjava: ok. 950–1450 LOC i 10–15 dni. Największe ryzyko to nie liczba linii, tylko bezpieczeństwo parsera odpowiedzi HTTP i DNS: oba parsują dane od nieznanego celu. Dlatego własny klient HTTP ma sens tylko z twardymi limitami (długość linii, liczba nagłówków, rozmiar ciała, czas) i testami na złych odpowiedziach; własny DNS tylko jeśli zero zależności jest wymogiem nadrzędnym.

## Rekomendacja

1. **HTTP: własny minimalny klient HTTP/1.1 na `Socket`/`SSLSocket`.** Uzasadnienie: JDK `HttpClient` nie daje faz ani przypięcia IP per żądanie [1][2][3][4]; OkHttp daje fazy, ale wprowadza Kotlin i 5 artefaktów [24][31]; HttpClient 5 nie ma per-request faz w rdzeniu [27]; Netty jest nieproporcjonalnie duży [33]. Własny klient jest ograniczony (GET/HEAD/POST, chunked, limit treści, przekierowania, TLS/SNI) i daje dokładnie te pomiary, których potrzebuje wykres faz. Jeśli właściciel uzna ryzyko parsera za zbyt duże, **OkHttp 5 jest najlepszym wariantem zapasowym**: `EventListener` + własny `Dns` + wyłączony pooling i przekierowania [24][25].
2. **DNS: dnsjava 3.7.1, używana bezpośrednio per check, nie jako globalne SPI.** Uzasadnienie: `InetAddress` nie ma TXT, per-check TTL i timeoutu, a natywny resolver może pinować [4][7]; własny parser DNS to 5–8 dni i ryzyko bezpieczeństwa; dnsjava obsługuje A/AAAA/TXT/TTL/cache i jest nieblokująca [29]. Nie używać jej jako `InetAddressResolverProvider`, bo SPI jest globalne i miesza polityki różnych checków [5][6].
3. **ICMP: brak na serwerze w MVP.** TCP connect do portu checka jest zamiennikiem dla „czy cel żyje”; `isReachable` jest best effort i często kończy się TCP na port 7 [4]. ICMP zostawić agentowi (backlog 2b). Opcjonalny tryb systemowego `ping` tylko jawnie włączany, z ostrzeżeniem o uprawnieniach [16][17][18].
4. **Certyfikaty: JDK.** `SSLSession.getPeerCertificates()` do prezentacji łańcucha, `X509ExtendedTrustManager`/`SSLContext` do walidacji i własnego CA, `SSLParameters` do SNI i weryfikacji nazwy [11][12][14]. Pamiętać, że `getPeerCertificates()` nie jest dowodem zaufania [11].
5. **Wątki wirtualne: własny klient na gniazdach + dnsjava.** Oba są VT-friendly; unikać `InetAddress.isReachable` i wbudowanego DNS na gorącej ścieżce [8][9][19][29]. `synchronized` nie jest już problemem w JDK 25 po JEP 491, ale nie trzymać go wokół I/O [9].
6. **Kategoria błędu: własny klasyfikator + cele kontrolne.** Biblioteki nie znają pojęcia „lokalizacja nie może wyjść”; mapowanie z tabeli wyżej i wzorzec zbiorczego błędu z sekcji 2 produktu.
7. **HTTP/2: nie w MVP.** HTTP/1.1 wystarcza; dla celów h2-only awaryjnie `HttpClient` lub OkHttp, z oznaczeniem, że fazy są przybliżone [1][22].

**Łączna lista zależności wprowadzanych przez rekomendację:**

| Nazwa | Wersja | Licencja | Zależności przechodnie runtime | Uwaga |
|---|---|---|---|---|
| `dnsjava:dnsjava` | 3.7.1 | BSD-3-Clause | 1: `org.slf4j:slf4j-api` 1.7.36 (już dostarczany przez Spring Boot); opcjonalnie `jna` 5.19.1 i `jna-platform` tylko na Windows | jedyna nowa biblioteka sieciowa [30] |
| — (HTTP, TLS, certyfikaty, ICMP, klasyfikacja) | — | — | 0 | wszystko na JDK 25 |

Łącznie: **jedna nowa zależność runtime** (`dnsjava`) i **zero nowych zależności przechodnich** w typowym wdrożeniu Spring Boot, bo `slf4j-api` już jest. Jeśli właściciel odrzuci dnsjava, wariant zero-dep to JNDI DNS dla TXT + `InetAddress` dla A/AAAA, z utratą per-check TTL i timeoutu.

## Ryzyka

| Ryzyko | Mitygacja |
|---|---|
| Błąd parsera HTTP u celu wrogim (request smuggling, ogromne nagłówki, powolne ciało) | twarde limity długości linii/nagłówków/ciała, timeouty, odrzucanie niepoprawnych odpowiedzi, testy na złych serwerach, brak obsługi nietypowych kodowań |
| Cel tylko HTTP/2 odrzuca HTTP/1.1 | wykryć i oznaczyć; fallback `HttpClient`/OkHttp tylko dla takich celów, z informacją „fazy przybliżone” |
| DNS rebinding przez drugi lookup | rozwiązać raz, walidować wszystkie adresy, połączyć się z zatwierdzonym IP, zachować Host/SNI/weryfikację [23] |
| dnsjava porzucona albo niezgodna z przyszłym JDK | BSD-3, aktywne wydania 3.7.1 (listopad 2026), mała powierzchnia API; alternatywa JNDI/własny resolver [29][30] |
| `slf4j-api` 1.7 vs 2.x w Spring Boot | egzekwować w Maven `dependencyManagement`; dnsjava działa z 1.7+ [30] |
| Pinning wątków w natywnym DNS, jeśli ktoś użyje `InetAddress` | nie używać wbudowanego resolvera na gorącej ścieżce; dnsjava nieblokująca; test JFR `jdk.VirtualThreadPinned` [8][9][29] |
| Brak ICMP ukrywa „host nieosiągalny, ale port otwarty” | TCP connect jest poprawnym testem usługi; dla czystego hosta dodać osobną semantykę lub agenta, nie udawać ICMP |
| 5 000 checków i świeże połączenie per check | osobny semafor per lokalizacja, budżet czasu, brak poolingu dla pomiaru faz; mierzyć koszt połączeń i ewentualnie współdzielić tylko dla checków bez faz |
| `getPeerCertificates()` zwraca niepełny łańcuch | do decyzji używać `TrustManager`, a łańcuch tylko prezentować; testy z niepełnym łańcuchem [11] |
| Własny klient TLS źle ustawia SNI przy przypiętym IP | `SSLSocketFactory.createSocket(Socket, host, port, true)` + `SSLParameters.setServerNames()` + `setEndpointIdentificationAlgorithm("HTTPS")` [12][13][14] |

## Pytania do grill-me

1. Czy akceptujesz własny parser HTTP/1.1 (800–1200 LOC, 8–12 dni) zamiast OkHttp, który daje `EventListener`, ale dokłada Kotlin i 5 artefaktów? Jeśli nie, czy OkHttp ma być rekomendacją główną?
2. Czy HTTP/2 jest wymagane w MVP, czy HTTP/1.1 wystarcza? Co robimy z celami, które odrzucają HTTP/1.1 — fallback `HttpClient` bez faz, OkHttp, czy oznaczamy jako nieobsługiwane?
3. Czy dnsjava (BSD-3, 1 przechodnia `slf4j-api`) może być jedyną nową zależnością sieciową, czy „zero zależności” jest twardym wymaganiem (wtedy JNDI DNS + `InetAddress` albo własny DNS)?
4. Czy serwer ma w ogóle wysyłać ICMP, czy TCP connect do portu checka wystarcza? Czy dopuszczasz opcjonalny tryb systemowego `ping` z ostrzeżeniem o `CAP_NET_RAW`/`ping_group_range`?
5. Jak klasyfikować niejednoznaczne przypadki (`UnknownHostException`, connect timeout, `SSLHandshakeException`), gdy cel kontrolny też pada? Jakie cele kontrolne konfigurujemy domyślnie i czy mają być per lokalizacja?
6. Czy pomiar faz ma być zawsze na świeżym połączeniu (bez poolingu), czy dopuszczamy reuse i wtedy oznaczamy fazy DNS/TCP/TLS jako „z cache”? Wpływa to na koszt przy 5 000 checków.
7. Czy certyfikaty sprawdzamy także dla celów niezweryfikowanych i self-signed, i czy własne CA per check ma być cache’owane globalnie, czy izolowane per przestrzeń?
8. Czy HTTP/2 i obsługa proxy wychodzącego są w MVP, czy odkładamy je do momentu, gdy pojawi się pierwszy realny cel, którego nie da się zmierzyć po HTTP/1.1?

## Źródła

- <https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.html>
- <https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpResponse.html>
- <https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpRequest.html>
- <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/net/InetAddress.html>
- <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/net/spi/InetAddressResolverProvider.html>
- <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/net/spi/InetAddressResolver.html>
- <https://openjdk.org/jeps/418>
- <https://openjdk.org/jeps/444>
- <https://openjdk.org/jeps/491>
- <https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html>
- <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/javax/net/ssl/SSLSession.html>
- <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/javax/net/ssl/SSLParameters.html>
- <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/javax/net/ssl/SSLSocket.html>
- <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/javax/net/ssl/X509ExtendedTrustManager.html>
- <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/net/package-summary.html>
- <https://man7.org/linux/man-pages/man7/icmp.7.html>
- <https://man7.org/linux/man-pages/man7/capabilities.7.html>
- <https://docs.docker.com/engine/containers/run/>
- <https://mail.openjdk.org/pipermail/core-libs-dev/2025-April/143552.html>
- <https://www.rfc-editor.org/rfc/rfc9110.html>
- <https://www.rfc-editor.org/rfc/rfc9112.html>
- <https://www.rfc-editor.org/rfc/rfc9113.html>
- <https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html>
- <https://github.com/square/okhttp/blob/master/docs/features/events.md>
- <https://lysine.dev/okhttp/5.x/okhttp/okhttp3/-event-listener/index.html>
- <https://hc.apache.org/httpcomponents-client-5.6.x/quickstart.html>
- <https://hc.apache.org/httpcomponents-client-5.6.x/examples-observation.html>
- <https://netty.io/4.1/api/io/netty/handler/codec/http/HttpClientCodec.html>
- <https://github.com/dnsjava/dnsjava>
- <https://repo1.maven.org/maven2/dnsjava/dnsjava/3.7.1/dnsjava-3.7.1.pom>
- <https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/5.5.1/okhttp-5.5.1.pom>
- <https://repo1.maven.org/maven2/org/apache/httpcomponents/client5/httpclient5/5.6.5/httpclient5-5.6.5.pom>
- <https://repo1.maven.org/maven2/io/netty/netty-codec-http/4.1.141.Final/netty-codec-http-4.1.141.Final.pom>
- <https://github.com/laurentbh/icmp4j>
- <https://www.bouncycastle.org/>
