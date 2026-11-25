package dev.wakey.spaces.internal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

/** Derived queries over memberships, including the composite identifier parts. */
public interface SpaceMemberRepository extends ListCrudRepository<SpaceMemberEntity, SpaceMemberKey> {

    List<SpaceMemberEntity> findByKeySubject(String subject);

    List<SpaceMemberEntity> findByKeySpaceIdInAndKeySubject(Collection<UUID> spaceIds, String subject);
}
