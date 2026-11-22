package dev.wakey.leadership;

import dev.wakey.testsupport.PostgresIntegrationTest;

/**
 * Runs the shared {@link LeaderLeaseContract} against PostgreSQL.
 *
 * <p>The singleton {@code PostgreSQLContainer}, its service connection and the shared application
 * context live in {@link dev.wakey.testsupport.PostgresIntegrationTest}; this class only mixes in
 * the contract.
 */
class PostgresLeaderLeaseTest extends PostgresIntegrationTest implements LeaderLeaseContract {

    // All test methods are default methods of the contract; this class only selects the database.
}
