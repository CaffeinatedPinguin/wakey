package dev.wakey.leadership;

import dev.wakey.testsupport.PostgresIntegrationTest;

/** Runs the leader lease contract against PostgreSQL. */
class PostgresLeaderLeaseTest extends PostgresIntegrationTest implements LeaderLeaseContract {}
