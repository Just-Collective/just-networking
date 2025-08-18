package com.just.networking.impl.frame.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Function;

public class TCPFrameChannel implements AutoCloseable {

    private final TCPFrameReadChannel tcpFrameReadChannel;

    private final TCPFrameWriteChannel tcpFrameWriteChannel;

    public TCPFrameChannel(Function<ByteBuffer, Integer> reader, Function<ByteBuffer, Integer> writer) {
        this.tcpFrameReadChannel = new TCPFrameReadChannel(reader);
        this.tcpFrameWriteChannel = new TCPFrameWriteChannel(writer);
    }

    @Override
    public void close() {
        tcpFrameReadChannel.close();
        tcpFrameWriteChannel.close();
    }

    public ByteBuffer readFrame() throws IOException {
        return tcpFrameReadChannel.readFrame();
    }

    public void sendFrame(ByteBuffer payload) throws IOException {
        tcpFrameWriteChannel.sendFrame(payload);
    }

    public void flushWrites() throws IOException {
        tcpFrameWriteChannel.flushWrites();
    }
}
