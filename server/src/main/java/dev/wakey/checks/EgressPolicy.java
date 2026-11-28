package dev.wakey.checks;

import java.net.InetAddress;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * The one egress address policy every check applies before it connects. Public addresses pass,
 * private networks and CGNAT pass only on an instance that explicitly monitors a LAN, and
 * loopback, unspecified, link-local, metadata, multicast, broadcast and reserved addresses never
 * pass.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class EgressPolicy {

    private static final int IPV4_LENGTH = 4;

    private static final int EMBEDDED_IPV4_OFFSET = 12;

    private static final int METADATA_LAST_OCTET = 254;

    private final EgressProperties properties;

    /** Tells whether a single address may be used for an outgoing connection. */
    public boolean allows(InetAddress address) {
        @Nullable String reason = rejectionReason(address);
        if (reason == null) {
            return true;
        }
        log.debug("Blocked egress to address {}: {}", address.getHostAddress(), reason);
        return false;
    }

    /**
     * Tells whether every address of one name resolution may be used. One blocked address rejects
     * the whole answer, so a connection never falls back to a rejected address.
     */
    public boolean allowsAll(List<InetAddress> addresses) {
        boolean allAllowed = true;
        for (InetAddress address : addresses) {
            if (!allows(address)) {
                allAllowed = false;
            }
        }
        return allAllowed;
    }

    private @Nullable String rejectionReason(InetAddress address) {
        byte[] bytes = address.getAddress();
        @Nullable String reason;
        if (bytes.length == IPV4_LENGTH) {
            reason = ipv4Reason(bytes, 0);
        } else {
            int offset = embeddedIpv4Offset(bytes);
            reason = offset >= 0 ? ipv4Reason(bytes, offset) : ipv6Reason(bytes);
        }
        return reason;
    }

    private static int embeddedIpv4Offset(byte[] bytes) {
        boolean mapped = isIpv4Mapped(bytes);
        boolean nat64 = isNat64(bytes);
        boolean compatible = allZero(bytes, 0, EMBEDDED_IPV4_OFFSET);
        int offset = -1;
        if (mapped || nat64 || compatible) {
            offset = EMBEDDED_IPV4_OFFSET;
        }
        return offset;
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        boolean zeroPrefix = allZero(bytes, 0, 10);
        boolean ffPrefix = octet(bytes, 10) == 0xff && octet(bytes, 11) == 0xff;
        return zeroPrefix && ffPrefix;
    }

    private static boolean isNat64(byte[] bytes) {
        boolean prefix = octet(bytes, 0) == 0x00
                && octet(bytes, 1) == 0x64
                && octet(bytes, 2) == 0xff
                && octet(bytes, 3) == 0x9b;
        boolean zeroMiddle = allZero(bytes, 4, EMBEDDED_IPV4_OFFSET);
        return prefix && zeroMiddle;
    }

    private static boolean allZero(byte[] bytes, int from, int to) {
        boolean zero = true;
        for (int index = from; zero && index < to; index++) {
            zero = bytes[index] == 0;
        }
        return zero;
    }

    private @Nullable String ipv4Reason(byte[] bytes, int offset) {
        @Nullable String reason = specialIpv4Reason(bytes, offset);
        if (reason == null) {
            reason = linkLocalIpv4Reason(bytes, offset);
        }
        if (reason == null) {
            reason = documentationIpv4Reason(bytes, offset);
        }
        if (reason == null) {
            reason = privateIpv4Reason(bytes, offset);
        }
        return reason;
    }

    private static @Nullable String specialIpv4Reason(byte[] bytes, int offset) {
        int first = octet(bytes, offset);
        @Nullable String reason = null;
        if (first == 0) {
            reason = isUnspecified(bytes, offset) ? "unspecified" : "reserved";
        } else if (first == 127) {
            reason = "loopback";
        } else if (first >= 224 && first <= 239) {
            reason = "multicast";
        } else if (first >= 240) {
            reason = isBroadcast(bytes, offset) ? "broadcast" : "reserved";
        }
        return reason;
    }

    private static @Nullable String linkLocalIpv4Reason(byte[] bytes, int offset) {
        @Nullable String reason = null;
        if (isLinkLocalIpv4(bytes, offset)) {
            reason = isMetadataIpv4(bytes, offset) ? "cloud-metadata" : "link-local";
        }
        return reason;
    }

    private static @Nullable String documentationIpv4Reason(byte[] bytes, int offset) {
        @Nullable String reason = null;
        if (isDocumentationIpv4(bytes, offset)) {
            reason = "reserved";
        }
        return reason;
    }

    private @Nullable String privateIpv4Reason(byte[] bytes, int offset) {
        @Nullable String reason = null;
        if (!properties.allowPrivateNetworks()) {
            if (isPrivateIpv4(bytes, offset)) {
                reason = "private";
            } else if (isCgnat(bytes, offset)) {
                reason = "cgnat";
            }
        }
        return reason;
    }

    private static boolean isUnspecified(byte[] bytes, int offset) {
        return allZero(bytes, offset, offset + IPV4_LENGTH);
    }

    private static boolean isBroadcast(byte[] bytes, int offset) {
        boolean firstPair = octet(bytes, offset) == 0xff && octet(bytes, offset + 1) == 0xff;
        boolean secondPair = octet(bytes, offset + 2) == 0xff && octet(bytes, offset + 3) == 0xff;
        return firstPair && secondPair;
    }

    private static boolean isLinkLocalIpv4(byte[] bytes, int offset) {
        return octet(bytes, offset) == 169 && octet(bytes, offset + 1) == 254;
    }

    private static boolean isMetadataIpv4(byte[] bytes, int offset) {
        boolean linkLocal = isLinkLocalIpv4(bytes, offset);
        boolean host = octet(bytes, offset + 2) == 169 && octet(bytes, offset + 3) == METADATA_LAST_OCTET;
        return linkLocal && host;
    }

    private static boolean isDocumentationIpv4(byte[] bytes, int offset) {
        boolean testNet = octet(bytes, offset) == 192 && octet(bytes, offset + 1) == 0 && octet(bytes, offset + 2) == 2;
        boolean benchmark =
                octet(bytes, offset) == 198 && octet(bytes, offset + 1) == 51 && octet(bytes, offset + 2) == 100;
        boolean documentation =
                octet(bytes, offset) == 203 && octet(bytes, offset + 1) == 0 && octet(bytes, offset + 2) == 113;
        return testNet || benchmark || documentation;
    }

    private static boolean isPrivateIpv4(byte[] bytes, int offset) {
        int first = octet(bytes, offset);
        int second = octet(bytes, offset + 1);
        boolean ten = first == 10;
        boolean oneSeventyTwo = first == 172 && second >= 16 && second <= 31;
        boolean oneNinetyTwo = first == 192 && second == 168;
        return ten || oneSeventyTwo || oneNinetyTwo;
    }

    private static boolean isCgnat(byte[] bytes, int offset) {
        int first = octet(bytes, offset);
        int second = octet(bytes, offset + 1);
        return first == 100 && second >= 64 && second <= 127;
    }

    private @Nullable String ipv6Reason(byte[] bytes) {
        int first = octet(bytes, 0);
        int second = octet(bytes, 1);
        @Nullable String reason = null;
        if (isEc2Metadata(bytes)) {
            reason = "cloud-metadata";
        } else if (isPrivateIpv6(first, second)) {
            reason = properties.allowPrivateNetworks() ? null : "private";
        } else if (isLinkLocalIpv6(first, second)) {
            reason = "link-local";
        } else if (first == 0xff) {
            reason = "multicast";
        } else if (isDocumentationIpv6(bytes)) {
            reason = "reserved";
        }
        return reason;
    }

    private static boolean isEc2Metadata(byte[] bytes) {
        boolean prefix = octet(bytes, 0) == 0xfd
                && octet(bytes, 1) == 0x00
                && octet(bytes, 2) == 0x0e
                && octet(bytes, 3) == 0xc2;
        boolean suffix = octet(bytes, 14) == 0x02 && octet(bytes, 15) == 0x54;
        boolean zeroMiddle = allZero(bytes, 4, 14);
        return prefix && suffix && zeroMiddle;
    }

    private static boolean isPrivateIpv6(int first, int second) {
        boolean uniqueLocal = (first & 0xfe) == 0xfc;
        boolean siteLocal = first == 0xfe && (second & 0xc0) == 0xc0;
        return uniqueLocal || siteLocal;
    }

    private static boolean isLinkLocalIpv6(int first, int second) {
        return first == 0xfe && (second & 0xc0) == 0x80;
    }

    private static boolean isDocumentationIpv6(byte[] bytes) {
        return octet(bytes, 0) == 0x20 && octet(bytes, 1) == 0x01 && octet(bytes, 2) == 0x0d && octet(bytes, 3) == 0xb8;
    }

    private static int octet(byte[] bytes, int index) {
        return bytes[index] & 0xff;
    }
}
