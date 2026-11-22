package dev.wakey.messaging;

import dev.wakey.testsupport.PostgresIntegrationTest;

/** Runs the message queue contract against PostgreSQL. */
class PostgresMessageQueueTest extends PostgresIntegrationTest implements MessageQueueContract {}
