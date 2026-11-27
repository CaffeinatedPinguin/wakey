package dev.wakey.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

class EgressPolicyTest {

    private final EgressPolicy policy = new EgressPolicy(new EgressProperties(false));

    @Test
    void privateLoopbackAndMetadataAddressesAreBlocked() {
        // given
        List<String> blocked = List.of(
                "127.0.0.1",
                "::1",
                "0.0.0.0",
                "::",
                "10.1.2.3",
                "172.16.0.1",
                "192.168.1.1",
                "100.64.0.1",
                "169.254.1.1",
                "169.254.169.254",
                "224.0.0.1",
                "239.255.255.255",
                "255.255.255.255",
                "192.0.2.1",
                "198.51.100.1",
                "203.0.113.1",
                "2001:db8::1",
                "fc00::1",
                "fd12:3456:789a::1",
                "fd00:ec2::254",
                "fe80::1",
                "ff02::1");

        // when
        List<String> allowed = blocked.stream().filter(this::allows).toList();

        // then
        assertThat(allowed).isEmpty();
    }

    @Test
    void mappedAndNat64AddressesAreJudgedByTheEmbeddedIpv4() {
        // given
        List<String> blocked = List.of(
                "::ffff:127.0.0.1", "::ffff:10.0.0.1", "64:ff9b::7f00:1", "64:ff9b::a00:1", "64:ff9b::a9fe:a9fe");
        List<String> publicAddresses = List.of("::ffff:8.8.8.8", "64:ff9b::808:808", "64:ff9b::101:101");

        // when
        List<String> blockedAllowed = blocked.stream().filter(this::allows).toList();
        List<String> publicBlocked =
                publicAddresses.stream().filter(this::isBlocked).toList();

        // then
        assertThat(blockedAllowed).isEmpty();
        assertThat(publicBlocked).isEmpty();
    }

    @Test
    void publicAddressesAreAllowed() {
        // given
        List<String> publicAddresses =
                List.of("8.8.8.8", "1.1.1.1", "93.184.216.34", "2606:4700:4700::1111", "2001:4860:4860::8888");

        // when
        List<String> blocked = publicAddresses.stream().filter(this::isBlocked).toList();

        // then
        assertThat(blocked).isEmpty();
    }

    @Test
    void oneBlockedAddressBlocksTheWholeAnswer() {
        // given
        List<InetAddress> mixed = List.of(address("8.8.8.8"), address("2606:4700:4700::1111"), address("10.0.0.1"));
        List<InetAddress> publicOnly = List.of(address("8.8.8.8"), address("2606:4700:4700::1111"));

        // when
        boolean mixedAllowed = policy.allowsAll(mixed);
        boolean publicAllowed = policy.allowsAll(publicOnly);

        // then
        assertThat(mixedAllowed).isFalse();
        assertThat(publicAllowed).isTrue();
    }

    @Test
    void allowingPrivateNetworksNeverAllowsLoopbackOrMetadata() {
        // given
        EgressPolicy permissive = new EgressPolicy(new EgressProperties(true));

        // when
        boolean privateAllowed = permissive.allows(address("10.1.2.3"));
        boolean cgnatAllowed = permissive.allows(address("100.64.0.1"));
        boolean loopbackAllowed = permissive.allows(address("127.0.0.1"));
        boolean ipv6LoopbackAllowed = permissive.allows(address("::1"));
        boolean metadataAllowed = permissive.allows(address("169.254.169.254"));
        boolean ipv6MetadataAllowed = permissive.allows(address("fd00:ec2::254"));
        boolean multicastAllowed = permissive.allows(address("224.0.0.1"));

        // then
        assertThat(privateAllowed).isTrue();
        assertThat(cgnatAllowed).isTrue();
        assertThat(loopbackAllowed).isFalse();
        assertThat(ipv6LoopbackAllowed).isFalse();
        assertThat(metadataAllowed).isFalse();
        assertThat(ipv6MetadataAllowed).isFalse();
        assertThat(multicastAllowed).isFalse();
    }

    private boolean allows(String literal) {
        return policy.allows(address(literal));
    }

    private boolean isBlocked(String literal) {
        return !allows(literal);
    }

    private static InetAddress address(String literal) {
        return InetAddress.ofLiteral(literal);
    }
}
