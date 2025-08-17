package com.just.networking.impl.frame.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.just.networking.Connection;

public interface FrameReadLoopHandler<C extends Connection> {

    default void onConnect(C connection) throws IOException {}

    void onReceiveFrame(C connection, int length, ByteBuffer payload) throws IOException; // read-only slice

    default void onDisconnect(C connection) throws IOException {}
}
