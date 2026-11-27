package dev.wakey.checks.internal;

import dev.wakey.checks.ErrorCategory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.repository.ListCrudRepository;

/** Derived queries over the append-only measurement results of a check. */
public interface CheckResultRepository extends ListCrudRepository<CheckResultEntity, UUID> {

    Optional<CheckResultEntity> findFirstByCheckIdOrderByCheckedAtDescIdDesc(UUID checkId);

    List<CheckResultEntity> findByCheckIdOrderByCheckedAtDescIdDesc(UUID checkId, Limit limit);

    boolean existsByCheckIdAndCheckedAt(UUID checkId, Instant checkedAt);

    boolean existsByCheckIdAndErrorCategoryIsNull(UUID checkId);

    boolean existsByCheckIdAndErrorCategory(UUID checkId, ErrorCategory errorCategory);
}
