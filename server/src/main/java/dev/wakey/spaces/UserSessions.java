package dev.wakey.spaces;

/** Removes every session of one user; implemented on Spring Session outside the module's HTTP surface. */
public interface UserSessions {

    void invalidateAll(String subject);
}
