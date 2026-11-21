# 0003 — Wykonywanie checków sieciowych na serwerze

- Status: accepted
- Date: 2026-11-21
- Superseded-by: none

## Context

Wykres faz (DNS, TCP, TLS, pierwszy bajt) per lokalizacja i ochrona przed DNS rebinding wymagają klienta HTTP, który daje czasy faz pojedynczego żądania i pozwala przypiąć adres IP. `java.net.http.HttpClient` nie daje ani jednego, ani drugiego ([net-java.md](../research/net-java.md)). Własny klient HTTP odrzucono jako zbyt ryzykowny (parser danych z sieci). Stack: [0002-stack.md](0002-stack.md).

Jak robią inni: OpenStatus (fazy jak u nas) otwiera nowe połączenie przy każdym checku i negocjuje HTTP/2; Uptime Kuma otwiera nowe połączenie, tylko HTTP/1.1, bez faz; Gatus używa puli, bez faz.

## Decision

- Klient checków HTTP: Reactor Netty `HttpClient`, fazy z callbacków cyklu życia żądania. Bez WebFluxa; wątek wirtualny executora czeka na wynik.
- DNS: resolver Netty (`DnsNameResolver`) podpięty do klienta — A/AAAA/TXT, TTL, timeout na zapytanie. Bez dnsjava.
- Przypięcie adresu: nazwa rozwiązywana raz, wszystkie adresy walidowane polityką ruchu wychodzącego, połączenie tylko z zatwierdzonym IP przy zachowaniu `Host`, SNI i weryfikacji certyfikatu.
- Każdy check na nowym połączeniu (bez puli), żeby fazy były pełne.
- HTTP/2 negocjowane jak w przeglądarce (ALPN), inaczej HTTP/1.1; wersja protokołu zapisywana w wyniku.
- Serwer nie wysyła ICMP; check „ping” z serwera to TCP connect. Prawdziwy ping tylko z agenta Go.
- Certyfikaty i kategoria błędu: JDK (`X509ExtendedTrustManager`, `SSLParameters`) i własny klasyfikator z celami kontrolnymi.

## Consequences

- Fazy z callbacków trzeba złożyć w jedną strukturę wyniku; metryki Micrometer Reactor Netty to agregaty, nie oś czasu checku.
- Przypięcie IP z zachowaniem SNI w Reactor Netty wymaga spike'u na etapie 0/1.
- Nowe połączenie na check kosztuje handshake TLS (ok. 17–83/s przy 5 000 checków co 1–5 min).
- Z chmury nie da się pingować hosta bez otwartego portu; tylko przez agenta.
- Agent Go robi to samo (httptrace, ALPN, nowe połączenie); zgodność pilnują wspólne wektory testowe.

## Links

- [net-java.md](../research/net-java.md), [net-go.md](../research/net-go.md), [stack-server.md](../research/stack-server.md)
