package com.just.networking.impl.message.channel;

import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.just.networking.config.Config;
import com.just.networking.impl.message.Message;

public class TCPMessageChannel implements AutoCloseable {

    private static final int INITIAL_CAP = 8 * 1024;

    private static final int OUT_FRAME_CAP = 64 * 1024;

    private final TCPMessageReadChannel tcpMessageReadChannel;

    private final TCPMessageWriteChannel tcpMessageWriteChannel;

    public TCPMessageChannel(
        Config config,
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs,
        Supplier<ByteBuffer> frameReader,
        Consumer<ByteBuffer> frameWriter
    ) {
        this.tcpMessageReadChannel = new TCPMessageReadChannel(config, schema, streamCodecs, frameReader);
        this.tcpMessageWriteChannel = new TCPMessageWriteChannel(config, schema, streamCodecs, frameWriter);
    }

    @Override
    public void close() {}

    public void sendMessage(Message<?> message) {
        tcpMessageWriteChannel.sendMessage(message);
    }

    /** Force-send the current outbound frame if it has any messages staged. */
    public void flush() {
        tcpMessageWriteChannel.flush();
    }

    /**
     * Pull the next decoded message if available. If the current inbound frame runs out, fetch another from
     * {@code frameReader}.
     */
    public @Nullable Message<?> pollMessage() {
        return tcpMessageReadChannel.pollMessage();
    }
}
