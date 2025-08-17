package com.just.networking;

@FunctionalInterface
public interface ConnectionBroker<Target> {

    ConnectResult connect(Target ctx);

    sealed interface ConnectResult {

        record Connected(Connection connection) implements ConnectResult {}

        record Refused(String reason) implements ConnectResult {}

        record Failed(Throwable cause) implements ConnectResult {}
    }
}
