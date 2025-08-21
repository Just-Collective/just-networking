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
                ($, frame) -> decodeFrameToMessages(connection, handler, frame)
            );
        } finally {
            handler.onDisconnect(connection);
        }
    }

    private void decodeFrameToMessages(C connection, MessageReadLoopHandler<C> handler, ByteBuffer frame) {
        while (true) {
            // Need at least a typeID.
            if (frame.remaining() < Short.BYTES) {
                // frame fully consumed (or malformed tail).
                return;
            }

            frame.mark();

            var typeId = frame.getShort();

            @SuppressWarnings("unchecked")
            StreamCodec<Message<?>> codec = (StreamCodec<Message<?>>) codecs.get(typeId);
            if (codec == null) {
                handler.onUnknownType(connection, typeId, frame.asReadOnlyBuffer());
                // We cannot recover without knowing payload length; abort this frame.
                return;
            }

            var before = frame.position();

            try {
                // must consume exactly its payload
                var message = codec.decode(schema, frame);
                handler.onReceiveMessage(connection, message);
            } catch (Exception e) {
                // Provide remaining bytes of this message (unknown length); roll back to typeId for context
                frame.reset();
                handler.onDecodeError(connection, typeId, frame.asReadOnlyBuffer(), e);
                // abort this frame to avoid desync
                return;
            }

            // Safety check: codec must advance position
            if (frame.position() <= before) {
                handler.onDecodeError(
                    connection,
                    typeId,
                    frame.asReadOnlyBuffer(),
                    new IOException("Codec did not advance buffer; cannot delimit messages without lengths")
                );
                return;
            }

            if (!frame.hasRemaining()) {
                // done with this frame.
                return;
            }
        }
    }

}
