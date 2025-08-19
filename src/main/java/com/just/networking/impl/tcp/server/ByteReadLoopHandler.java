package com.just.networking.impl.tcp.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.just.networking.impl.tcp.TCPConnection;

public interface ByteReadLoopHandler<C extends TCPConnection> {

    default void onConnect(C connection) {}

    void onReceiveBytes(C connection, ByteBuffer data) throws IOException;

    default void onDisconnect(C connection) {}
}
