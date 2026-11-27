package dev.wakey.spaces.internal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.repository.ListCrudRepository;

/** Derived queries over memberships, including the composite identifier parts. */
public interface SpaceMemberRepository extends ListCrudRepository<SpaceMemberEntity, SpaceMemberKey> {

    List<SpaceMemberEntity> findByKeyIssuerAndKeySubject(String issuer, String subject);

    List<SpaceMemberEntity> findByKeySpaceIdInAndKeyIssuerAndKeySubject(
            Collection<UUID> spaceIds, String issuer, String subject);

    List<SpaceMemberEntity> findByKeySpaceIdAndKeyIssuerGreaterThanOrderByKeyIssuerAscKeySubjectAsc(
            UUID spaceId, String issuer, Limit limit);

    List<SpaceMemberEntity> findByKeySpaceIdAndKeyIssuerAndKeySubjectGreaterThanOrderByKeySubjectAsc(
            UUID spaceId, String issuer, String subject, Limit limit);
}
