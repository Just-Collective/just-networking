package com.just.networking.impl.msg;

import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.just.networking.Transport;
import com.just.networking.impl.frame.TCPFrameTransport;

public class TCPMessageTransport implements Transport {

    private final TCPFrameTransport tcpFrameTransport;

    private final TCPMessageChannel tcpMessageChannel;

    public TCPMessageTransport(
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs,
        TCPFrameTransport tcpFrameTransport
    ) {
        this.tcpFrameTransport = tcpFrameTransport;
        this.tcpMessageChannel = new TCPMessageChannel(
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

    public Message<?> pollMessage() {
        return tcpMessageChannel.pollMessage();
    }

    public void flushWrites() throws IOException {
        tcpFrameTransport.flushWrites();
    }

    @Override
    public void close() throws IOException {
        tcpFrameTransport.close();
    }
}
