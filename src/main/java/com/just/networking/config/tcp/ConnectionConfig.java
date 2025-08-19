package com.just.networking.config.tcp;

public record ConnectionConfig(
    boolean allowHalfClose
) {

    public static final ConnectionConfig DEFAULT = new ConnectionConfig(false);

    public ConnectionConfig withAllowHalfClose(boolean allowHalfClose) {
        return new ConnectionConfig(allowHalfClose);
    }
}
