package com.just.networking.impl.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.just.networking.Transport;
import com.just.networking.config.frame.TCPFrameConfig;
import com.just.networking.impl.frame.channel.TCPFrameChannel;
import com.just.networking.impl.tcp.TCPTransport;

public class TCPFrameTransport implements Transport {

    private final TCPTransport tcpTransport;

    private final TCPFrameChannel tcpFrameChannel;

    public TCPFrameTransport(TCPFrameConfig tcpFrameConfig, TCPTransport tcpTransport) {
        this.tcpTransport = tcpTransport;
        this.tcpFrameChannel = new TCPFrameChannel(tcpFrameConfig, byteBuffer -> {
            try {
                return tcpTransport.read(byteBuffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, byteBuffer -> {
            try {
                return tcpTransport.write(byteBuffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
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
