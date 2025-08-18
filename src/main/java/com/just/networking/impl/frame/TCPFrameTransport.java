package com.just.networking.impl.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.just.networking.ChannelIO;
import com.just.networking.Transport;
import com.just.networking.impl.tcp.TCPTransport;

public class TCPFrameTransport implements Transport {

    private final TCPTransport tcpTransport;

    private final TCPFrameChannel tcpFrameChannel;

    public TCPFrameTransport(TCPTransport tcpTransport) {
        this.tcpTransport = tcpTransport;
        this.tcpFrameChannel = new TCPFrameChannel(new ChannelIO() {

            @Override
            public int read(ByteBuffer dst) throws IOException {
                return tcpTransport.read(dst);
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                return tcpTransport.write(src);
            }
        });
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

    public void flushWrites() throws IOException {
        tcpFrameChannel.flushWrites();
    }

    @Override
    public void close() throws IOException {
        tcpTransport.close();
    }
}
