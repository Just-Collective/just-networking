package com.just.networking.impl.msg.client;

import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import com.just.networking.ConnectionBroker;
import com.just.networking.impl.frame.client.TCPFrameClient;
import com.just.networking.impl.msg.Message;

public class MsgClient implements AutoCloseable {

    // start size; grows as needed
    private static final int INITIAL_CAP = 8 * 1024;

    private static final ByteOrder ORDER = ByteOrder.BIG_ENDIAN;

    // Reusable scratch buffer for encoding [typeId + payload].
    // Not thread-safe: use one MsgClient per I/O thread.
    private final ByteBuffer scratchBuffer;

    private final StreamCodecSchema<ByteBuffer> schema;

    private final TCPFrameClient TCPFrameClient;

    private final Map<Short, StreamCodec<? extends Message<?>>> streamCodecs;

    public MsgClient(Map<Short, StreamCodec<? extends Message<?>>> streamCodecs, StreamCodecSchema<ByteBuffer> schema) {
        this(streamCodecs, schema, new TCPFrameClient());
    }

    public MsgClient(
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs,
        StreamCodecSchema<ByteBuffer> schema,
        TCPFrameClient TCPFrameClient
    ) {
        this.streamCodecs = streamCodecs;
        this.TCPFrameClient = TCPFrameClient;
        this.schema = schema;
        this.scratchBuffer = ByteBuffer.allocateDirect(INITIAL_CAP).order(ORDER);
    }

    @Override
    public void close() {
        disconnect();
    }

    public ConnectionBroker.ConnectResult connect(String host, int port) {
        return TCPFrameClient.connect(host, port);
    }

    public void sendMessage(Message<?> message) throws IOException {
        var typeId = message.id();

        @SuppressWarnings("unchecked")
        var codec = (StreamCodec<Message<?>>) streamCodecs.get(typeId);

        if (codec == null) {
            throw new IllegalArgumentException("Unknown message type: " + typeId);
        }

        scratchBuffer.clear();
        scratchBuffer.putShort(typeId);
        codec.encode(schema, scratchBuffer, message);

        scratchBuffer.flip();

        TCPFrameClient.sendFrame(scratchBuffer);
    }

    public Message<?> pollMessage() throws IOException {
        var frame = TCPFrameClient.readFrame();

        if (frame == null) {
            return null;
        }

        // Parse type id first.
        var typeId = frame.getShort();

        var codec = streamCodecs.get(typeId);

        if (codec == null) {
            throw new IllegalArgumentException("Unknown type id on wire: " + typeId);
        }

        // Decode remainder.
        return codec.decode(schema, frame);
    }

    public void disconnect() {
        TCPFrameClient.disconnect();
    }

    public void flushWrites() throws IOException {
        TCPFrameClient.flushWrites();
    }

    public boolean isOpen() {
        return TCPFrameClient.isOpen();
    }
}
