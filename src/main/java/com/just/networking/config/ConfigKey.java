package com.just.networking.config;

public sealed interface ConfigKey<T> {

    record Nullable<T>(String id) implements ConfigKey<T> {}

    record Default<T>(
        String id,
        T defaultValue
    ) implements ConfigKey<T> {}
}
