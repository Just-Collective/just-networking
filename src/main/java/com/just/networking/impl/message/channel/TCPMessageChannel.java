package com.just.networking.impl.message.channel;

import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.just.networking.config.message.TCPMessageConfig;
import com.just.networking.impl.message.Message;

public class TCPMessageChannel implements AutoCloseable {

    // start size; grows as needed
    private static final int INITIAL_CAP = 8 * 1024;

    private static final ByteOrder ORDER = ByteOrder.BIG_ENDIAN;

    private final StreamCodecSchema<ByteBuffer> schema;

    private final Map<Short, StreamCodec<? extends Message<?>>> streamCodecs;

    // Underlying I/O.
    private final Consumer<ByteBuffer> frameWriter;

    private final Supplier<ByteBuffer> frameReader;

    // Reusable scratch buffer for encoding [typeId + payload].
    // Not thread-safe: use one MsgClient per I/O thread.
    private final ByteBuffer scratchBuffer;

    public TCPMessageChannel(
        TCPMessageConfig tcpMessageConfig,
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs,
        Supplier<ByteBuffer> frameReader,
        Consumer<ByteBuffer> frameWriter
    ) {
        this.schema = schema;
        this.streamCodecs = streamCodecs;
        this.frameReader = frameReader;
        this.frameWriter = frameWriter;
        this.scratchBuffer = ByteBuffer.allocateDirect(INITIAL_CAP).order(ORDER);
    }

    @Override
    public void close() {}

    public void sendMessage(Message<?> message) {
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

        frameWriter.accept(scratchBuffer);
    }

    public @Nullable Message<?> pollMessage() {
        var frame = frameReader.get();

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
}
