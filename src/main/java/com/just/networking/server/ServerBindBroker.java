package com.just.networking.server;

import java.io.IOException;

@FunctionalInterface
public interface ServerBindBroker<T, SC extends ServerConnection<?>> {

    SC bind(T target) throws IOException;
}
