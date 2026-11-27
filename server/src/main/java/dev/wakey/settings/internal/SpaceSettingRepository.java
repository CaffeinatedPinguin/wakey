package dev.wakey.settings.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

/** Derived reads over space settings; writes go through the aggregate template. */
interface SpaceSettingRepository extends ListCrudRepository<SpaceSettingEntity, SpaceSettingKey> {

    List<SpaceSettingEntity> findByKeySpaceId(UUID spaceId);
}
