package com.just.networking.server;

import java.io.IOException;

import com.just.networking.Connection;

public interface ServerConnection<C extends Connection> extends AutoCloseable {

    boolean isOpen();

    C accept() throws IOException;

    @Override
    void close() throws IOException;
}
