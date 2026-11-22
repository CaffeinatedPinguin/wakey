package dev.wakey.leadership;

import dev.wakey.testsupport.HsqldbIntegrationTest;

/** Runs the leader lease contract against HSQLDB. */
class HsqldbLeaderLeaseTest extends HsqldbIntegrationTest implements LeaderLeaseContract {}
