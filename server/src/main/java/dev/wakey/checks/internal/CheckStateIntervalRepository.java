package dev.wakey.checks.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

/** Derived queries over the state intervals of a check. */
public interface CheckStateIntervalRepository extends ListCrudRepository<CheckStateIntervalEntity, UUID> {

    Optional<CheckStateIntervalEntity> findFirstByCheckIdAndEndedAtIsNullOrderByStartedAtDesc(UUID checkId);
}
