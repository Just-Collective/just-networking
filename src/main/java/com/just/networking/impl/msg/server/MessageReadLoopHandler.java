package com.just.networking.impl.msg.server;

import java.nio.ByteBuffer;

import com.just.networking.Connection;
import com.just.networking.impl.msg.Message;

public interface MessageReadLoopHandler<C extends Connection> {

    default void onConnect(C connection) {}

    /** Called for each successfully decoded message. */
    void onReceiveMessage(C connection, Message<?> message);

    /** Called when a frame's typeId has no matching codec. Payload is read-only. */
    default void onUnknownType(C connection, short typeId, ByteBuffer payload) {}

    /** Called when decoding fails for a known type. Payload is read-only (remaining bytes for that frame). */
    default void onDecodeError(C connection, short typeId, ByteBuffer payload, Exception error) {}

    default void onDisconnect(C connection) {}
}
