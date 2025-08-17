package com.just.networking.impl.msg.server;

import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.just.networking.impl.frame.server.TcpFrameReadLoop;
import com.just.networking.impl.msg.Message;
import com.just.networking.impl.tcp.TCPConnection;
import com.just.networking.server.ReadLoop;

public final class TcpMessageReadLoop<C extends TCPConnection> implements ReadLoop<C, MessageReadLoopHandler<C>> {

    private final Map<Short, StreamCodec<? extends Message<?>>> codecs;

    private final StreamCodecSchema<ByteBuffer> schema;

    private final int maxFrameSize;

    public TcpMessageReadLoop(
        Map<Short, StreamCodec<? extends Message<?>>> codecs,
        StreamCodecSchema<ByteBuffer> schema
    ) {
        this(codecs, schema, 8 * 1024 * 1024);
    }

    public TcpMessageReadLoop(
        Map<Short, StreamCodec<? extends Message<?>>> codecs,
        StreamCodecSchema<ByteBuffer> schema,
        int maxFrameSize
    ) {
        this.codecs = codecs;
        this.schema = schema;
        this.maxFrameSize = Math.max(1024, maxFrameSize);
    }

    @Override
    public void run(C connection, MessageReadLoopHandler<C> handler) throws IOException {
        // Reuse the existing framed loop; we only provide a small adapter handler.
        var framed = new TcpFrameReadLoop<C>(maxFrameSize);

        handler.onConnect(connection);

        try {
            framed.run(connection, (conn, length, frame) -> {
                // 'frame' contains exactly one frame's bytes: [typeId (2) | payload]
                if (frame.remaining() < Short.BYTES) {
                    // malformed frame; report as decode error
                    handler.onDecodeError(
                        conn,
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
                    handler.onUnknownType(conn, typeId, frame.asReadOnlyBuffer());
                    return;
                }

                try {
                    // Decode from the remaining bytes of the frame.
                    var msg = codec.decode(schema, frame);
                    handler.onReceiveMessage(conn, msg);
                } catch (Exception e) {
                    // Hand the remaining (post-typeId) payload to help with diagnostics.
                    handler.onDecodeError(conn, typeId, frame.asReadOnlyBuffer(), e);
                }
            });
        } finally {
            handler.onDisconnect(connection);
        }
    }
}
