package com.just.networking.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Config {

    public static final Config DEFAULT = Config.builder().build();

    public static Builder builder() {
        return new Builder();
    }

    private final Map<ConfigKey<?>, Object> properties;

    private Config(Map<ConfigKey<?>, Object> properties) {
        this.properties = properties;
    }

    public <T> T get(ConfigKey.Default<T> configKey) {
        return getOrDefault(configKey, configKey.defaultValue());
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(ConfigKey<T> configKey, T defaultValue) {
        return (T) properties.getOrDefault(configKey, defaultValue);
    }

    public Map<ConfigKey<?>, Object> getEntries() {
        return properties;
    }

    public Builder toBuilder() {
        return new Builder(properties);
    }

    public static class Builder {

        private final Map<ConfigKey<?>, Object> properties;

        private Builder() {
            this(Map.of());
        }

        private Builder(Map<ConfigKey<?>, Object> properties) {
            this.properties = new HashMap<>(properties);
        }

        public <T> Builder with(ConfigKey<T> configKey, T value) {
            properties.put(configKey, value);
            return this;
        }

        public Config build() {
            return new Config(Collections.unmodifiableMap(properties));
        }
    }
}
