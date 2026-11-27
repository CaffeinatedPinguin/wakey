package dev.wakey.checks.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.repository.ListCrudRepository;

/** Derived queries over checks; the aggregate is read and written only through this repository. */
public interface CheckRepository extends ListCrudRepository<CheckEntity, UUID> {

    Optional<CheckEntity> findByIdAndSpaceId(UUID id, UUID spaceId);

    long countBySpaceId(UUID spaceId);

    @Lock(LockMode.PESSIMISTIC_WRITE)
    Optional<CheckEntity> findForUpdateById(UUID id);
}
