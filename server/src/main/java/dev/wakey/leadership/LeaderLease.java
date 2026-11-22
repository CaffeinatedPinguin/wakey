package dev.wakey.leadership;

import java.util.Optional;

public interface LeaderLease {

    Optional<LeaseGrant> acquireOrRenew(String name);

    void release(String name);
}
