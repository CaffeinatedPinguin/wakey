package dev.wakey.messaging;

public record QueuedMessage(long cursor, String topic, String key, String payload) {}
