package com.just.networking.impl.message.channel;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.just.networking.config.Config;
import com.just.networking.impl.message.Message;
import com.just.networking.impl.message.MessageAccess;

public class TCPMessageChannel implements AutoCloseable {

    private static final int INITIAL_CAP = 8 * 1024;

    private static final int OUT_FRAME_CAP = 64 * 1024;

    private final TCPMessageReadChannel tcpMessageReadChannel;

    private final TCPMessageWriteChannel tcpMessageWriteChannel;

    public TCPMessageChannel(
        Config config,
        MessageAccess messageAccess,
        Supplier<ByteBuffer> frameReader,
        Consumer<ByteBuffer> frameWriter
    ) {
        this.tcpMessageReadChannel = new TCPMessageReadChannel(config, messageAccess, frameReader);
        this.tcpMessageWriteChannel = new TCPMessageWriteChannel(config, messageAccess, frameWriter);
    }

    @Override
    public void close() {}

    public void sendMessage(Message message) {
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
    public @Nullable Message pollMessage() {
        return tcpMessageReadChannel.pollMessage();
    }
}
