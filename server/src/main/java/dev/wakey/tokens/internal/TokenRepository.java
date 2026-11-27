package dev.wakey.tokens.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

/** Derived queries over tokens; the hash lookup rides the unique index. */
public interface TokenRepository extends ListCrudRepository<TokenEntity, UUID> {

    Optional<TokenEntity> findByTokenHash(String tokenHash);
}
