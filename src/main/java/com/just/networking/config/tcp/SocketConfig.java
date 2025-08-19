package com.just.networking.config.tcp;

import com.bvanseg.just.functional.option.Option;

public record SocketConfig(
    boolean blocking,
    boolean tcpNoDelay,
    boolean keepAlive,
    Option<Integer> soLinger,
    Option<Integer> connectTimeoutMillis
) {

    public static final SocketConfig DEFAULT = new SocketConfig(true, true, true, Option.none(), Option.none());

    public SocketConfig withBlocking(boolean blocking) {
        return new SocketConfig(blocking, tcpNoDelay, keepAlive, soLinger, connectTimeoutMillis);
    }
}
