package com.just.networking.impl.message.server;

import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.just.networking.impl.frame.server.TCPFrameReadLoop;
import com.just.networking.impl.message.Message;
import com.just.networking.impl.message.TCPMessageConnection;
import com.just.networking.server.ReadLoop;

public final class TCPMessageReadLoop<C extends TCPMessageConnection> implements ReadLoop<C, MessageReadLoopHandler<C>> {

    private final StreamCodecSchema<ByteBuffer> schema;

    private final Map<Short, StreamCodec<? extends Message<?>>> codecs;

    public TCPMessageReadLoop(
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> codecs
    ) {
        this.schema = schema;
        this.codecs = codecs;
    }

    @Override
    public void run(C connection, MessageReadLoopHandler<C> handler) {
        // Reuse the existing framed loop; we only provide a small adapter handler.
        var tcpFrameReadLoop = new TCPFrameReadLoop<>();

        handler.onConnect(connection);

        try {
            tcpFrameReadLoop.run(
                connection.asTCPFrameConnection(),
                ($, frame) -> decodeFrameToMessage(connection, handler, frame)
            );
        } finally {
            handler.onDisconnect(connection);
        }
    }

    private void decodeFrameToMessage(C connection, MessageReadLoopHandler<C> handler, ByteBuffer frame) {
        // 'frame' contains exactly one frame's bytes: [typeId (2) | payload]
        if (frame.remaining() < Short.BYTES) {
            // malformed frame; report as decode error
            handler.onDecodeError(
                connection,
                (short) 0,
                frame.asReadOnlyBuffer(),
                new IOException("Frame too short for typeId: " + frame.remaining() + " bytes")
            );
            return;
        }

        short typeId = frame.getShort();

        @SuppressWarnings("unchecked")
        var codec = (StreamCodec<Message<?>>) codecs.get(typeId);

        if (codec == null) {
            // Expose the remaining payload read-only to the app
            handler.onUnknownType(connection, typeId, frame.asReadOnlyBuffer());
            return;
        }

        try {
            // Decode from the remaining bytes of the frame.
            var message = codec.decode(schema, frame);
            handler.onReceiveMessage(connection, message);
        } catch (Exception e) {
            // Hand the remaining (post-typeId) payload to help with diagnostics.
            handler.onDecodeError(connection, typeId, frame.asReadOnlyBuffer(), e);
        }
    }
}
