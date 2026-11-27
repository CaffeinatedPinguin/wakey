package dev.wakey.checks.internal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

/** Derived queries over tag rows; the aggregate writes tags through its own mapped collection. */
public interface CheckTagRepository extends ListCrudRepository<CheckTagRow, CheckTagKey> {

    List<CheckTagRow> findBySpaceIdAndKeyTag(UUID spaceId, String tag);

    List<CheckTagRow> findByKeyCheckIdIn(Collection<UUID> checkIds);
}
