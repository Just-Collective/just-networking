package com.just.networking.impl.frame.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.just.networking.impl.frame.TCPFrameConnection;

public interface FrameReadLoopHandler<C extends TCPFrameConnection> {

    default void onConnect(C connection) {}

    void onReceiveFrame(C connection, ByteBuffer payload) throws IOException;

    default void onDisconnect(C connection) {}
}
