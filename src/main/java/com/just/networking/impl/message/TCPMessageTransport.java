package com.just.networking.impl.message;

import com.bvanseg.just.functional.result.Result;
import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.just.networking.Transport;
import com.just.networking.config.Config;
import com.just.networking.impl.frame.TCPFrameTransport;
import com.just.networking.impl.message.channel.TCPMessageChannel;

public class TCPMessageTransport implements Transport {

    private final TCPFrameTransport tcpFrameTransport;

    private final TCPMessageChannel tcpMessageChannel;

    public TCPMessageTransport(
        Config config,
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs,
        TCPFrameTransport tcpFrameTransport
    ) {
        this.tcpFrameTransport = tcpFrameTransport;
        this.tcpMessageChannel = new TCPMessageChannel(
            config,
            schema,
            streamCodecs,
            () -> {
                try {
                    return tcpFrameTransport.readFrame();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },
            byteBuffer -> {
                try {
                    tcpFrameTransport.sendFrame(byteBuffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    @Override
    public boolean isOpen() {
        return tcpFrameTransport.isOpen();
    }

    public void sendMessage(Message<?> message) {
        tcpMessageChannel.sendMessage(message);
    }

    public @Nullable Message<?> pollMessage() {
        return tcpMessageChannel.pollMessage();
    }

    public void flushWrites() throws IOException {
        // Flush messages to the frame channel.
        tcpMessageChannel.flush();
        // Flush frames to the tcp socket.
        tcpFrameTransport.flushWrites();
    }

    @Override
    public Result<Void, IOException> closeWithResult() {
        return tcpFrameTransport.closeWithResult();
    }
}
