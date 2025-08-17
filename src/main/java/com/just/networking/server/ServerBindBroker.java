package com.just.networking.server;

import java.io.IOException;

@FunctionalInterface
public interface ServerBindBroker<Target, Connection extends ServerConnection> {

    Connection bind(Target target) throws IOException;
}
