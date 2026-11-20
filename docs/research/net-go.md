# Research: agent wakey w Go — pomiary sieciowe, dystrybucja i parity

## Zakres

Badam implementację agenta wakey w Go (backlog pkt 2b): ICMP bez roota, fazy HTTP, lokalną listę dozwolonych celów, kanał agent→serwer, format konfiguracji, dystrybucję, wydania/podpisy i parity z executorem Java. Zakres obejmuje osiem potrzeb opisanych w specyfikacji; każdą traktuję osobno i pokazuję wariant „platforma/własny kod” oraz wariant biblioteczny. Poza zakresem zostają: kanał przez proxy i firewalle (backlog pkt 8), mTLS (v1), Windows/macOS, szczegóły UI oraz wybór bibliotek frontendu. Wersje tez: standardowa biblioteka Go z dokumentacji `go1.27.4`, `golang.org/x/net` v0.63.0 (wydane 2026-11-17); stan źródeł na 2026-11-20. To research, nie decyzja: rekomendacje i kompromisy wymagają grill-me z właścicielem.

## Opcje

### 1. ICMP bez roota

**Platforma / własny kod.** `golang.org/x/net/icmp` nie jest częścią stdlib, ale jest utrzymywanym subrepozytorium Go. `icmp.ListenPacket("udp4", ...)` i `"udp6"` otwierają gniazda datagramowe ICMP; dokumentacja mówi wprost, że tryb nieuprzywilejowany obsługują obecnie tylko Darwin i Linux, a przykład ostrzega o `net.ipv4.ping_group_range` ([pkg.go.dev/icmp](https://pkg.go.dev/golang.org/x/net/icmp)). Na Linuksie warunkiem jest zakres grup w tym sysctl; pro-bing pokazuje dokładne polecenie `net.ipv4.ping_group_range="0 2147483647"` oraz alternatywę `setcap cap_net_raw=+ep` ([pro-bing](https://github.com/prometheus-community/pro-bing)). W Dockerze odpowiadają temu `--sysctl net.ipv4.ping_group_range=...` i `--cap-add NET_RAW`, oba dostępne w `docker run` ([Docker run](https://docs.docker.com/reference/cli/docker/container/run/)). Własny kod musi: wykryć błąd `listen` (odmowa uprawnień), spróbować `ip4:icmp`/`ip6:ipv6-icmp` tylko gdy agent ma `CAP_NET_RAW`, a po niepowodzeniu przejść na TCP connect i oznaczyć wynik kategorią „lokalizacja nie może wyjść do sieci”, nie „cel nie odpowiada”. Dopasowanie odpowiedzi wymaga pilnowania ID i Seq; `icmp.Echo` ma pola `ID`, `Seq`, `Data` ([pkg.go.dev/icmp](https://pkg.go.dev/golang.org/x/net/icmp)).

```go
c, err := icmp.ListenPacket("udp4", "0.0.0.0")
if err != nil {
    // EPERM/EACCES: brak ping_group_range; spróbuj ip4:icmp tylko z CAP_NET_RAW
}
defer c.Close()
```

**Biblioteka (wariant alternatywny).** `github.com/prometheus-community/pro-bing` (Apache-2.0) robi to samo i dodaje statystyki, obsługę duplikatów oraz `HTTPCaller`; jego `go.mod` wymaga `google/uuid`, `x/net`, `x/sync` i pośrednio `x/sys` ([go.mod pro-bing](https://raw.githubusercontent.com/prometheus-community/pro-bing/main/go.mod)). `github.com/go-ping/ping` (MIT) jest w README jawnie oznaczony jako nieutrzymywany i wskazuje pro-bing jako następcę ([go-ping](https://github.com/go-ping/ping)) — dla nowego projektu to ryzyko.

### 2. Fazy HTTP

**Platforma / własny kod.** `net/http/httptrace` daje hooki `DNSStart`/`DNSDone` (z `Addrs` i `Err`), `ConnectStart`/`ConnectDone`, `TLSHandshakeStart`/`TLSHandshakeDone` (z `tls.ConnectionState`), `GotConn` (z `GotConnInfo.Reused`) i `GotFirstResponseByte` ([pkg.go.dev/httptrace](https://pkg.go.dev/net/http/httptrace)). Dokumentacja zastrzega, że trace obejmuje pojedynczy `RoundTrip` i nie obejmuje łańcucha przekierowań; hooki mogą być wołane współbieżnie. To wystarcza na fazy DNS, TCP, TLS i pierwszy bajt, ale wymaga własnego połączenia czasów w jedną strukturę wyniku. `GotConnInfo.Reused` pozwala oznaczyć pomiar jako „połączenie reużywane”; dla HTTP/2 hook `PutIdleConn` nie jest używany, a `Transport` nie włącza HTTP/2 domyślnie — robi to `DefaultTransport` ([pkg.go.dev/net/http](https://pkg.go.dev/net/http)).

**Własny `Dialer` i przypięcie adresu.** `net.Resolver` z `PreferGo` i `Dialer.Resolver` pozwalają rozwiązać nazwę samodzielnie; po wybraniu adresu należy dialować `ip:port`, a w `tls.Config` ustawić `ServerName` na pierwotną nazwę hosta. Bez tego weryfikacja certyfikatu sprawdza IP, a nie nazwę. Standardowy resolver na Unixie preferuje czysty Go, bo blokowane DNS zajmuje tylko goroutine ([pkg.go.dev/net](https://pkg.go.dev/net)). Łańcuch certyfikatów: `tls.ConnectionState.PeerCertificates` i `VerifiedChains`; własne CA przez `x509.SystemCertPool()`, `Clone()`, `AppendCertsFromPEM()` i `tls.Config.RootCAs` ([pkg.go.dev/x509](https://pkg.go.dev/crypto/x509)). `x509.Certificate.Verify` i `VerifyHostname` pozwalają policzyć dni do wygaśnięcia i sprawdzić nazwę niezależnie od `tls.Config`.

**Biblioteka (wariant alternatywny).** `pro-bing` zawiera `HTTPCaller` oparty na `httptrace`, z API bezpiecznym współbieżnie i własnymi timerami faz ([pro-bing](https://github.com/prometheus-community/pro-bing)). Zaletą jest gotowy model danych, wadą — dodatkowa zależność i mniejsza kontrola nad przypięciem adresu oraz `ServerName`.

### 3. Lokalna lista dozwolonych celów

**Platforma / własny kod.** `net/netip` wystarcza: `netip.ParsePrefix("10.0.0.0/8")`, `Prefix.Contains(addr)`, `netip.ParseAddr`, `Addr.Unmap()` dla adresów IPv4-mapped i `Addr.Compare` do sortowania ([pkg.go.dev/net/netip](https://pkg.go.dev/net/netip)). `Addr` jest wartością porównywalną i może być kluczem mapy, więc lista hostów to zwykła mapa. Ważne zastrzeżenie z dokumentacji: `Addr.IsPrivate()` „nie opisuje właściwości bezpieczeństwa i nie powinno być używane do kontroli dostępu” — listę trzeba budować z jawnych prefiksów, nie z `IsPrivate`. `Prefix.Contains` nie dopasuje IPv4-mapped IPv6 do prefiksu IPv4 i zwraca `false` dla adresu ze strefą, więc przed sprawdzeniem trzeba wywołać `Unmap()`.

**Własny kod.** Lista CIDR + dokładne nazwy hostów; sprawdzanie po rozwiązaniu DNS i ponownie na adresie, do którego faktycznie dialujemy (ochrona przed rebindingiem). Odrzucenie celu musi trafić do UI z powodem, zgodnie z produktem (`docs/specs/product-features.md`, sekcja 2). Szacunek: 100–150 linii, 1–2 dni, testy tabelaryczne na IPv4, IPv6, mapped i strefach.

**Biblioteka (wariant alternatywny).** `github.com/yl2chen/cidranger` (MIT) to trie CIDR do szybkiego `Contains` przy dużych listach ([cidranger](https://github.com/yl2chen/cidranger)); `go4.org/netipx` (BSD-3-Clause) oferuje `IPSet` na `net/netip` ([netipx](https://github.com/go4org/netipx)). Obie są małe, ale dla list rzędu setek prefiksów liniowy przegląd `netip.Prefix` jest prawdopodobnie szybszy we wdrożeniu i nie wnosi zależności.

### 4. Kanał agent → serwer w MVP

**Platforma / własny kod.** `net/http` + własna pętla ponowień: limit prób lub budżet czasu, wykładniczy odstęp z losowym rozrzutem, potem stan „uśpiony” (jedna próba co 5 minut), zgodnie z sekcją 0 produktu. `429` i nagłówek `Retry-After` mają dwa formaty: delta-seconds i HTTP-date; `http.ParseTime` obsłuży datę, a `strconv.Atoi` sekundy. Semantykę `Retry-After` definiuje RFC 9110 §10.2.3 ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110#section-10.2.3)). Bufor: w MVP ograniczony ring w pamięci; jeśli wynik musi przetrwać restart, append-only plik JSON Lines z `fsync` i atomową rotacją. `encoding/json` wystarcza; nie ma potrzeby sięgać po bibliotekę retry.

**Biblioteka (wariant alternatywny).** `github.com/hashicorp/go-retryablehttp` (MPL-2.0) opakowuje `net/http`, dodaje ponowienia przy błędach połączenia i odpowiedziach 5xx (poza 501), backoff oraz „przewijanie” body ([go-retryablehttp](https://github.com/hashicorp/go-retryablehttp)). Nie potwierdziłem w źródle, że obsługuje `Retry-After` i stan „uśpiony”; to i tak wymagałoby własnej warstwy. Biblioteka ma sens głównie wtedy, gdy potrzebne jest bezpieczne ponawianie POST-ów z dużym body.

### 5. Konfiguracja lokalna agenta

**Platforma / własny kod (JSON).** `encoding/json` jest w stdlib, ma `Decoder.DisallowUnknownFields()`, obsługę wersji schema i czytelne błędy pozycji. Minusy: brak komentarzy i końcowych przecinków, więc ręczna edycja jest mniej wygodna. Dla pliku z listą CIDR i kilkoma polami to jednak najmniejszy koszt utrzymania.

**Biblioteka YAML.** `gopkg.in/yaml.v3` (MIT/Apache-2.0) wspiera komentarze i anchors, ale ostatnie wydanie v3.0.1 pochodzi z 2022-05-27 ([pkg.go.dev/yaml.v3](https://pkg.go.dev/gopkg.in/yaml.v3)); dla projektu na 10 lat to sygnał ryzyka (brak aktywnego maintainera, większa powierzchnia parsera).

**Biblioteka TOML.** `github.com/BurntSushi/toml` (MIT) wspiera TOML v1.1.0, ma walidator `tomlv`, `MetaData.Undecoded()` i wydanie v1.6.0 z 2025-12-18 ([pkg.go.dev/toml](https://pkg.go.dev/github.com/BurntSushi/toml)). Jest bardziej „konfiguracyjny” niż JSON, ale to kolejna zależność i kolejny format do dokumentacji.

### 6. Dystrybucja

**Platforma / własny kod.** `CGO_ENABLED=0 GOOS=linux GOARCH=amd64` (i `arm64`) daje statyczną binarkę. `import _ "time/tzdata"` dokłada bazę stref czasowych do binarki (ok. 450 KB) i jest używane tylko wtedy, gdy system nie ma tzdata ([pkg.go.dev/time/tzdata](https://pkg.go.dev/time/tzdata)). Dla CA: `x509.SystemCertPool` czyta systemowy bundle, a `x509.SetFallbackRoots` pozwala wbudować awaryjny zestaw w kontenerze bez systemowych certyfikatów ([pkg.go.dev/x509](https://pkg.go.dev/crypto/x509)). Obraz `gcr.io/distroless/static` zawiera `ca-certificates`, `tzdata`, `/etc/passwd` i `/tmp`, więc jest wygodniejszy niż `scratch` ([distroless](https://github.com/GoogleContainerTools/distroless/blob/main/base/README.md)). `scratch` wymaga skopiowania bundle CA i tzdata albo polegania na `time/tzdata` i wbudowanych rootach.

**Docker.** `--network=host` daje pomiar z sieci hosta (bez NAT bridge, z jego DNS i MTU), ale zmniejsza izolację i jest opcją linuksową; domyślny bridge mierzy z sieci kontenera i może zmienić wynik (NAT, polityki, DNS). `--cap-add NET_RAW` i `--sysctl` są dostępne w `docker run` ([Docker run](https://docs.docker.com/reference/cli/docker/container/run/)). Różnicę sieci kontenera i hosta trzeba opisać w dokumentacji agenta, bo inaczej użytkownik porówna nieporównywalne pomiary.

**Biblioteka / gotowe narzędzie.** GoReleaser v2.20 buduje macierz `GOOS/GOARCH`, ustawia `CGO_ENABLED=0`, `-trimpath`, `mod_timestamp` i publikuje artefakty ([GoReleaser Go](https://goreleaser.com/customization/builds/builders/go/)). Alternatywą jest `ko` do obrazów bez Dockera. Oba są narzędziami wydania, nie zależnościami runtime.

### 7. Wydania bez ciężkich narzędzi

**Platforma / własny kod.** `go build` + Makefile lub skrypt: `-trimpath`, `-buildvcs=false` (albo świadome `true`), `CGO_ENABLED=0`, `sha256sum`, podpis. `-trimpath` usuwa ścieżki systemowe z binarki, a `-buildvcs` kontroluje stemplowanie informacjami VCS ([pkg.go.dev/cmd/go](https://pkg.go.dev/cmd/go)). Dla powtarzalności: brak `main.Date` w `-ldflags`, stały `mod_timestamp`, brak wywołań czasu w templatach; GoReleaser opisuje dokładnie te pułapki ([GoReleaser Go](https://goreleaser.com/customization/builds/builders/go/)).

**Warianty podpisów.**
- `minisign`: Ed25519, jeden plik `.minisig`, publiczny klucz w jednej linii; weryfikacja `minisign -Vm plik -P ...` ([minisign](https://jedisct1.github.io/minisign/)). Najprostszy dla użytkownika binarki.
- `ssh-keygen -Y sign -n file`: używa istniejących kluczy SSH, weryfikacja przez `allowed_signers`; namespace chroni przed myleniem kontekstów ([ssh-keygen](https://man.openbsd.org/ssh-keygen)). Wygodne, jeśli użytkownik już ma SSH.
- `cosign`/Sigstore: keyless przez OIDC, Fulcio i Rekor; standard dla kontenerów, ale wymaga sieci, zaufania do TUF i logu przejrzystości ([Sigstore](https://docs.sigstore.dev/cosign/signing/overview/)).
- GPG: powszechny, ale największy narzut operacyjny (klucze, keyring, web-of-trust).

**Biblioteka / gotowe narzędzie.** GoReleaser ma wbudowane integracje podpisów i artefaktów, ale dodaje konfigurację i własny cykl wydania ([GoReleaser](https://goreleaser.com/)). Dla jednej binarki i dwóch architektur prostszy jest Makefile + minisign; GoReleaser wygrywa, gdy dochodzą pakiety, obrazy i wiele kanałów publikacji.

### 8. Parity z serwerem Java

**Platforma / własny kod.** Jedno źródło prawdy: pliki JSON w repo, np. `testdata/parity/http.json`, `testdata/parity/errors.json`. Każdy wektor ma `id`, wejście (metoda, URL, nagłówki, body, odpowiedź serwera), oczekiwany wynik asercji, kod błędu i kategorię (`target_down` / `location_network_error`). Go czyta je `encoding/json` i uruchamia jako testy tabelaryczne; Java uruchamia ten sam plik w JUnit 5 `@ParameterizedTest` (Jackson jest już w Spring Boot). Kontrakt to dane, nie wspólny kod — zgodnie z ADR 0001 („kontrakt JSON i testy parity”).

**Zasady wektorów.** (1) Kategoria błędu to zamknięta lista stringów, nie tekst wyjątku; (2) asercje HTTP opisują kod, obecność/brak słowa kluczowego, ścieżkę JSON i nagłówek; (3) każdy wektor ma wersję schematu; (4) brakujące pola są błędem, nie domyślną wartością; (5) ten sam plik jest kopiowany lub pobierany z jednego modułu, nigdy duplikowany ręcznie. Dla porównania faz tolerancja musi być jawna (np. ±50 ms), inaczej testy będą niestabilne.

**Biblioteka (wariant alternatywny).** `github.com/google/go-cmp` (BSD-3-Clause) daje czytelne diffy struktur w testach Go ([go-cmp](https://github.com/google/go-cmp)); `github.com/santhosh-tekuri/jsonschema/v6` (Apache-2.0) może walidować pliki wektorów względem schematu ([jsonschema](https://github.com/santhosh-tekuri/jsonschema)). Oba są test-only; dla małego zestawu wystarczy `encoding/json` + `reflect.DeepEqual`.

## Porównanie

### Wybór wariantu per potrzeba

| Potrzeba | Platforma / własny kod | Wariant biblioteczny | KISS (mniej = lepiej) |
|---|---|---|---|
| ICMP | `x/net/icmp` `udp4`/`udp6` + fallback TCP | pro-bing (Apache-2.0) | własny, jeśli testy uprawnień są w CI |
| Fazy HTTP | `httptrace` + własny `Dialer`/`Resolver` | pro-bing `HTTPCaller` | własny dla kontroli pinningu i TLS |
| Lista celów | `net/netip` prefiksy + mapa hostów | cidranger (MIT) / netipx (BSD-3) | własny przy setkach prefiksów |
| Kanał | `net/http` + własna pętla + JSON | go-retryablehttp (MPL-2.0) | własny dla `Retry-After` i stanu „uśpiony” |
| Konfiguracja | `encoding/json` | yaml.v3 (MIT/Apache-2.0), BurntSushi/toml (MIT) | JSON, jeśli ręczna edycja nie jest kluczowa |
| Dystrybucja | `go build` + distroless/scratch | GoReleaser / ko | Makefile + distroless static |
| Wydania | Makefile + minisign | GoReleaser + cosign/ssh-keygen | minisign dla binarki |
| Parity | JSON + testy tabelaryczne | go-cmp, jsonschema (test-only) | JSON + stdlib |

### ICMP: biblioteki i koszt

| Opcja | Licencja | Zależności runtime | Aktywność | Ryzyko 10 lat |
|---|---|---|---|---|
| `x/net/icmp` + własny kod | BSD-3-Clause | `x/sys` (1 realnie linkowany; go.mod x/net wymaga 4) | wydania Go, subrepo | niskie: utrzymuje Go team |
| pro-bing | Apache-2.0 | 3 bezpośrednie + 1 pośrednia | 510 gwiazdek, Prometheus community | średnie: kolejny maintainer |
| go-ping | MIT | 2 bezpośrednie | README: nieutrzymywany | wysokie: brak wsparcia |

### Kanał, konfiguracja i wydania

| Kryterium | Stdlib / własne | Biblioteka / narzędzie |
|---|---|---|
| Ponowienia i backoff | pełna kontrola, ~300 linii | retryablehttp: gotowe, ale bez stanu „uśpiony” |
| `429 Retry-After` | własny parser, RFC 9110 | niepotwierdzone w retryablehttp |
| Bufor wyników | pamięć lub własny WAL | brak standardowej biblioteki |
| JSON | `encoding/json`, zero zależności | — |
| YAML/TOML | — | kolejny parser i CVE |
| Podpis | minisign / ssh-keygen | cosign: standard, ale OIDC + Rekor |
| Reproducible | `-trimpath`, `-buildvcs=false`, stały timestamp | GoReleaser robi to z pudełka |

## Własna implementacja

Warto napisać samemu: dopasowanie ICMP (ID/Seq, timeout, duplikaty), klasyfikację błędów „cel” vs „lokalizacja”, pinning DNS i `ServerName`, listę dozwolonych, pętlę ponowień ze stanem „uśpiony” i bufor, oraz runner wektorów parity. Nie warto pisać własnego parsera YAML/TOML ani własnej kryptografii podpisów.

| Moduł | Szacunek linii | Dni 1 osoby | Trudne miejsca | Jak testować |
|---|---:|---:|---|---|
| ICMP + fallback TCP | 200–300 | 3–5 | uprawnienia w Dockerze, IPv6, fałszywe reply | kontener bez `NET_RAW`, z `--sysctl`, host |
| Fazy HTTP + TLS | 300–500 | 5–7 | reużycie połączeń, HTTP/2, pinning, własne CA | serwer testowy, certyfikaty self-signed, CDN |
| Lista dozwolonych | 100–150 | 1–2 | DNS rebinding, IPv4-mapped, strefy | testy tabelaryczne CIDR/host |
| Kanał + bufor | 250–400 | 4–6 | 429, jitter, restart, częściowy zapis | serwer fault-injection, zabicie procesu |
| Konfiguracja JSON | 80–120 | 1 | walidacja i wersjonowanie | pliki poprawne i błędne |
| Wydania + podpisy | 100–200 (skrypty) | 2–3 | klucze, reproducible, weryfikacja użytkownika | build dwa razy, `sha256sum`, minisign |
| Wektory parity | 100–200 JSON + 100 Go/Java | 3–5 | różnice semantyki Java/Go | uruchomienie tych samych plików w CI |

Razem: około 1100–1900 linii i 19–29 dni jednej osoby, jeśli wszystko powstaje od zera. To potwierdza, że „własny kod” wygrywa tylko dlatego, że każdy moduł jest mały i izolowany; biblioteki skracają czas, ale dodają dług utrzymania.

## Rekomendacja

1. **ICMP:** `x/net/icmp` z `udp4`/`udp6` i własnym fallbackiem TCP. Uzasadnienie: to ten sam mechanizm, który owija pro-bing, a my potrzebujemy własnej klasyfikacji błędów i decyzji o uprawnieniach; pro-bing nie zniknie, ale nie jest wart zależności dla 200–300 linii.
2. **Fazy HTTP:** `httptrace` + własny `Dialer`/`Resolver` z pinningiem i `tls.Config.ServerName`, własne CA przez `x509`. Uzasadnienie: kontrola nad adresem i nazwą certyfikatu jest krytyczna dla bezpieczeństwa; `HTTPCaller` z pro-bing nie daje pełnej kontroli.
3. **Lista dozwolonych:** `net/netip` + własna lista CIDR i dokładnych hostów. Uzasadnienie: setki prefiksów nie wymagają trie; mniej kodu i zero zależności.
4. **Kanał:** `net/http` + własna pętla ponowień, `Retry-After` i bufor w pamięci (opcjonalnie plik JSON Lines). Uzasadnienie: stan „uśpiony” i trwałość bufora są specyficzne dla produktu; retryablehttp nie pokrywa tej semantyki.
5. **Konfiguracja:** JSON w stdlib. Uzasadnienie: najmniejsza powierzchnia ataku i zero zależności; jeśli ręczna edycja okaże się bólem, TOML jest lepszy od YAML (aktywny maintainer, walidator).
6. **Dystrybucja:** statyczna binarka + `time/tzdata` + distroless static; `--network=host` jako opcja udokumentowana, nie domyślna. Uzasadnienie: distroless daje CA i tzdata bez `scratch`-owych pułapek.
7. **Wydania:** Makefile + `go build` z `-trimpath -buildvcs=false` + minisign; cosign tylko dla obrazów kontenera, jeśli będą publikowane. Uzasadnienie: najprostsza weryfikacja dla użytkownika i najmniej ruchomych części.
8. **Parity:** JSON w repo + testy tabelaryczne Go i JUnit. Uzasadnienie: kontrakt danych jest trwalszy niż wspólny kod i nie wymaga nowych zależności produkcyjnych.

### Łączna lista zależności

Lista minimalna po `go mod tidy` (przewidywana; nie potwierdzona wykonaniem, bo w tym zadaniu nie buduję kodu):

| Moduł | Wersja | Typ | Licencja | Zależności przechodnie | Uzasadnienie |
|---|---|---|---|---|---|
| `golang.org/x/net` | v0.63.0 | bezpośrednia | BSD-3-Clause | `go.mod` wymaga `x/crypto`, `x/sys`, `x/term`, `x/text` (4); dla `icmp`/`ipv4`/`ipv6` linkowany jest `x/sys` (1) | ICMP `udp4`/`udp6`; `pkg.go.dev` potwierdza API i licencję |
| `golang.org/x/sys` | v0.50.0 | pośrednia | BSD-3-Clause | 0 | wymagany przez `x/net` dla syscalli |

Poza `go.mod`: brak zależności runtime. Testy Go: stdlib (`encoding/json`, `reflect`, `testing`). Wydanie: `minisign` jako narzędzie poza repo; opcjonalnie `cosign` dla obrazów. Nie rekomenduję pro-bing, retryablehttp, yaml.v3, BurntSushi/toml, cidranger, netipx, go-cmp ani jsonschema w wersji MVP.

## Ryzyka

| Ryzyko | Mitygacja |
|---|---|
| `ping_group_range` niedostępny w kontenerze | wykryć błąd `listen`, fallback TCP, czytelny komunikat i test w CI bez `NET_RAW` |
| Fałszywy „DOWN” z powodu uprawnień | kategoria „lokalizacja” zamiast „cel”; cele kontrolne w pliku agenta |
| Pinning adresu psuje CDN lub rotację | dokumentować, testować wiele adresów, nie pinować przy każdym checku bez potrzeby |
| Własne CA i `ServerName` mylą się przy IP | testy certyfikatów self-signed i IP SAN; `VerifyHostname` jawnie |
| HTTP/2 i reużycie zmieniają czasy faz | oznaczać `Reused` i wersję protokołu; tolerancje w parity |
| Bufor w pamięci ginie przy restarcie | jawna decyzja produktowa; opcjonalny WAL JSON Lines z `fsync` |
| YAML/TOML: CVE i porzucone repo | JSON w MVP; TOML tylko po decyzji i z pinem |
| Reproducible build zależy od środowiska | `-trimpath`, `-buildvcs=false`, stały timestamp, build dwa razy w CI |
| Klucz minisign/cosign wycieknie | klucz offline, rotacja, publikacja odcisku w repo i na stronie |
| Parity testuje za dużo lub za mało | zamknięta lista kategorii, wersja schematu, tolerancja czasu, przegląd wektorów |

## Pytania do grill-me

1. Czy agent w MVP musi obsługiwać ICMP po IPv6, czy wystarczy IPv4 i TCP fallback?
2. Czy akceptujemy `net.ipv4.ping_group_range` przez `--sysctl` w obrazie, czy wolimy `CAP_NET_RAW` i prostszy obraz?
3. Czy domyślnym trybem w Dockerze ma być `--network=host`, czy bridge z jawnie udokumentowanym zniekształceniem pomiaru?
4. Czy bufor wyników musi przetrwać restart agenta, czy utrata bufora przy restarcie jest akceptowalna?
5. Czy plik konfiguracji ma być edytowany ręcznie przez administratora (wtedy TOML/YAML), czy generowany przez instalator (wtedy JSON)?
6. Czy podpis wydań ma być minisign (prosty dla binarki), czy cosign/Sigstore (kontenery i transparentność)?
7. Czy parity ma porównywać dokładne teksty błędów, czy tylko stabilne kody i kategorie?
8. Czy lokalna lista dozwolonych ma dopuszczać nazwy hostów, czy wyłącznie CIDR i adresy IP?
9. Czy pomiary HTTP mają domyślnie wymuszać HTTP/1.1 dla porównywalności faz, czy dopuszczać HTTP/2?
10. Czy agent ma mieć w MVP wbudowany awaryjny bundle CA, czy polegać wyłącznie na systemie/distroless?

## Źródła

- https://pkg.go.dev/golang.org/x/net/icmp — `ListenPacket`, `udp4`/`udp6`, `icmp.Echo`, ograniczenie Darwin/Linux, `ping_group_range` w przykładzie.
- https://pkg.go.dev/golang.org/x/net/ipv4 — pakiet IP-level, `ICMPType`, `PacketConn`, example tracing.
- https://pkg.go.dev/net/http/httptrace — hooki DNS/Connect/TLS/GotConn/GotFirstResponseByte, ograniczenia trace.
- https://pkg.go.dev/net/http — `Transport`, `StatusTooManyRequests`, HTTP/2 i `DefaultTransport`.
- https://pkg.go.dev/net — resolver czysty Go vs cgo, `Dialer`, `Resolver`.
- https://pkg.go.dev/net/netip — `Addr`, `Prefix`, `Contains`, `Unmap`, ostrzeżenie o `IsPrivate`.
- https://pkg.go.dev/time/tzdata — wbudowana baza stref, ok. 450 KB, `timetzdata`.
- https://pkg.go.dev/crypto/x509 — `SystemCertPool`, `SetFallbackRoots`, `Verify`, `VerifyHostname`.
- https://pkg.go.dev/cmd/go — `-trimpath`, `-buildvcs`, flagi budowania.
- https://pkg.go.dev/gopkg.in/yaml.v3 — YAML v3, licencja, wydanie v3.0.1.
- https://pkg.go.dev/github.com/BurntSushi/toml — TOML v1.1.0, `MetaData`, wydanie v1.6.0.
- https://raw.githubusercontent.com/golang/net/master/go.mod — wymagania modułu `x/net` (x/crypto, x/sys, x/term, x/text).
- https://raw.githubusercontent.com/prometheus-community/pro-bing/main/go.mod — zależności pro-bing.
- https://github.com/prometheus-community/pro-bing — ICMP bez roota, sysctl, `setcap`, `HTTPCaller`.
- https://github.com/go-ping/ping — MIT, jawne oznaczenie „Deprecated”.
- https://github.com/hashicorp/go-retryablehttp — MPL-2.0, retry/backoff, 5xx.
- https://github.com/yl2chen/cidranger — MIT, trie CIDR.
- https://github.com/go4org/netipx — BSD-3-Clause, `IPSet` na `net/netip`.
- https://github.com/google/go-cmp — BSD-3-Clause, diffy w testach.
- https://github.com/santhosh-tekuri/jsonschema — Apache-2.0, walidacja JSON Schema.
- https://docs.docker.com/reference/cli/docker/container/run/ — `--sysctl`, `--cap-add`, `--network`.
- https://github.com/GoogleContainerTools/distroless/blob/main/base/README.md — zawartość `static`: CA, tzdata, `/etc/passwd`, `/tmp`.
- https://goreleaser.com/ — GoReleaser v2.20, wydania, podpisy, pakiety.
- https://goreleaser.com/customization/builds/builders/go/ — macierz buildów, `CGO_ENABLED=0`, `-trimpath`, `mod_timestamp`.
- https://docs.sigstore.dev/cosign/signing/overview/ — keyless, Fulcio, Rekor, TUF.
- https://jedisct1.github.io/minisign/ — Ed25519, `-G`, `-S`, `-V`, trusted comments.
- https://man.openbsd.org/ssh-keygen — `-Y sign`, `-Y verify`, namespace, allowed signers.
- https://www.rfc-editor.org/rfc/rfc9110#section-10.2.3 — `Retry-After`, delta-seconds i HTTP-date.
