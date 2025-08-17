package com.just.networking.impl.tcp.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.just.networking.Connection;

public interface ByteReadLoopHandler<C extends Connection> {

    default void onConnect(C connection) throws IOException {}

    void onReceiveBytes(C connection, ByteBuffer data) throws IOException; // read-only slice

    default void onDisconnect(C connection) throws IOException {}
}
