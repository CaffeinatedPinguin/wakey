package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMemberId;
import dev.wakey.spaces.SpaceSlug;
import dev.wakey.web.ApiException;
import dev.wakey.web.ApiPaths;
import dev.wakey.web.CommonError;
import dev.wakey.web.CursorCodec;
import java.net.URI;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

/** The cursor and link helpers of the spaces HTTP surface. */
final class SpacesHttp {

    static final String PATH = ApiPaths.API_V1 + "/spaces";

    static final String MEMBERS_PATH = PATH + "/{slug}/members";

    static final String MEMBER_ROLE_PATH = MEMBERS_PATH + "/role";

    static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private static final String MEMBER_SEPARATOR = ":";

    private SpacesHttp() {}

    static URI location(String slug) {
        return UriComponentsBuilder.fromPath(PATH).pathSegment(slug).build().toUri();
    }

    static String afterSlug(@Nullable String cursor) {
        return cursor == null ? "" : sortValue(cursor);
    }

    static String memberSortKey(SpaceMember member) {
        String issuer = member.issuer();
        return issuer.length() + MEMBER_SEPARATOR + issuer + member.subject();
    }

    static SpaceMemberId afterMember(@Nullable String cursor) {
        if (cursor == null) {
            return new SpaceMemberId("", "");
        }
        return decodeMember(CursorCodec.decode(cursor));
    }

    private static String sortValue(String encoded) {
        String sortValue = CursorCodec.decode(encoded);
        if (!SpaceSlug.isValid(sortValue)) {
            throw new ApiException(CommonError.INVALID_CURSOR);
        }
        return sortValue;
    }

    private static SpaceMemberId decodeMember(String key) {
        int separator = key.indexOf(MEMBER_SEPARATOR);
        if (separator < 1) {
            throw new ApiException(CommonError.INVALID_CURSOR);
        }
        int issuerLength = parseLength(key.substring(0, separator));
        int subjectStart = separator + 1 + issuerLength;
        if (issuerLength < 1 || subjectStart > key.length()) {
            throw new ApiException(CommonError.INVALID_CURSOR);
        }
        String issuer = key.substring(separator + 1, subjectStart);
        String subject = key.substring(subjectStart);
        if (!SpaceMember.isValidIssuer(issuer) || !SpaceMember.isValidSubject(subject)) {
            throw new ApiException(CommonError.INVALID_CURSOR);
        }
        return new SpaceMemberId(issuer, subject);
    }

    private static int parseLength(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new ApiException(CommonError.INVALID_CURSOR, exception);
        }
    }
}
