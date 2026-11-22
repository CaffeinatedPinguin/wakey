package dev.wakey.messaging;

import dev.wakey.testsupport.HsqldbIntegrationTest;

/** Runs the message queue contract against HSQLDB. */
class HsqldbMessageQueueTest extends HsqldbIntegrationTest implements MessageQueueContract {}
