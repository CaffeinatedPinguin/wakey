package dev.wakey.spaces.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.repository.ListCrudRepository;

/** Derived queries over spaces; callers never hand-write SQL for this aggregate. */
public interface SpaceRepository extends ListCrudRepository<SpaceEntity, UUID> {

    Optional<SpaceEntity> findBySlug(String slug);

    List<SpaceEntity> findBySlugGreaterThanOrderBySlug(String slug, Limit limit);

    List<SpaceEntity> findByIdInAndSlugGreaterThanOrderBySlug(Collection<UUID> ids, String slug, Limit limit);

    /** Locks one space row until the surrounding transaction ends; a missing row is empty. */
    @Lock(LockMode.PESSIMISTIC_WRITE)
    Optional<SpaceEntity> findForUpdateById(UUID id);
}
