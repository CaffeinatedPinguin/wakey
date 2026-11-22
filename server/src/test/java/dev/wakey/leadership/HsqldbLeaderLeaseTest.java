package dev.wakey.leadership;

import dev.wakey.testsupport.HsqldbIntegrationTest;

/**
 * Runs the shared {@link LeaderLeaseContract} against the HSQLDB in-memory database.
 *
 * <p>The datasource properties and the shared application context live in
 * {@link dev.wakey.testsupport.HsqldbIntegrationTest}; this class only mixes in the contract.
 */
class HsqldbLeaderLeaseTest extends HsqldbIntegrationTest implements LeaderLeaseContract {

    // All test methods are default methods of the contract; this class only selects the database.
}
