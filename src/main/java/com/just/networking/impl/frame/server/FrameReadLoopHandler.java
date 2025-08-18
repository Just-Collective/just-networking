package com.just.networking.impl.frame.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.just.networking.impl.frame.TCPFrameConnection;

public interface FrameReadLoopHandler<C extends TCPFrameConnection> {

    default void onConnect(C connection) {}

    void onReceiveFrame(C connection, ByteBuffer payload);

    default void onDisconnect(C connection) {}

    default void onFrameReadError(C connection, IOException exception) {}
}
