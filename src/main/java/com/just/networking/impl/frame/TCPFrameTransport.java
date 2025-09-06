package com.just.networking.impl.frame;

import com.just.core.functional.result.Result;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.just.networking.Transport;
import com.just.networking.config.Config;
import com.just.networking.impl.frame.channel.TCPFrameChannel;
import com.just.networking.impl.tcp.TCPTransport;

public class TCPFrameTransport implements Transport {

    private final TCPTransport tcpTransport;

    private final TCPFrameChannel tcpFrameChannel;

    public TCPFrameTransport(Config config, TCPTransport tcpTransport) {
        this.tcpTransport = tcpTransport;
        this.tcpFrameChannel = new TCPFrameChannel(config, byteBuffer -> {
            try {
                return tcpTransport.read(byteBuffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, tcpTransport);
    }

    @Override
    public boolean isOpen() {
        return tcpTransport.isOpen();
    }

    public ByteBuffer readFrame() throws IOException {
        return tcpFrameChannel.readFrame();
    }

    public void sendFrame(ByteBuffer payload) throws IOException {
        tcpFrameChannel.sendFrame(payload);
    }

    public void sendFrameAndFlush(ByteBuffer payload) throws IOException {
        sendFrame(payload);
        flushWrites();
    }

    public void flushWrites() throws IOException {
        tcpFrameChannel.flushWrites();
    }

    @Override
    public Result<Void, IOException> closeWithResult() {
        return tcpTransport.closeWithResult();
    }
}
