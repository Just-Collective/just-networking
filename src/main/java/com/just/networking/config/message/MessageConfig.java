package com.just.networking.config.message;

public record MessageConfig(
    boolean failOnUnknownType
) {

    public static final MessageConfig DEFAULT = new MessageConfig(true);

    public MessageConfig withFailOnUnknownType(boolean failOnUnknownType) {
        return new MessageConfig(failOnUnknownType);
    }
}
