package com.just.networking.impl.frame.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Function;

import com.just.networking.Writer;
import com.just.networking.config.frame.TCPFrameConfig;

public class TCPFrameChannel implements AutoCloseable {

    private final TCPFrameReadChannel tcpFrameReadChannel;

    private final TCPFrameWriteChannel tcpFrameWriteChannel;

    public TCPFrameChannel(
        TCPFrameConfig tcpFrameConfig,
        Function<ByteBuffer, Integer> reader,
        Writer writer
    ) {
        this.tcpFrameReadChannel = new TCPFrameReadChannel(tcpFrameConfig, reader);
        this.tcpFrameWriteChannel = new TCPFrameWriteChannel(tcpFrameConfig, writer);
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
