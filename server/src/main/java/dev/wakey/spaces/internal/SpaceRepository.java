package dev.wakey.spaces.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.repository.ListCrudRepository;

/** Derived queries over spaces; callers never hand-write SQL for this aggregate. */
public interface SpaceRepository extends ListCrudRepository<SpaceEntity, UUID> {

    Optional<SpaceEntity> findBySlug(String slug);

    List<SpaceEntity> findBySlugGreaterThanOrderBySlug(String slug, Limit limit);

    List<SpaceEntity> findByIdInAndSlugGreaterThanOrderBySlug(Collection<UUID> ids, String slug, Limit limit);
}
