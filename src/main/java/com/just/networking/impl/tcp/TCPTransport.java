package com.just.networking.impl.tcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.just.networking.Transport;

public class TCPTransport implements Transport {

    private final SocketChannel socketChannel;

    public TCPTransport(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public boolean isOpen() {
        return socketChannel.isOpen();
    }

    public int read(ByteBuffer dst) throws IOException {
        return socketChannel.read(dst);
    }

    public int write(ByteBuffer src) throws IOException {
        return socketChannel.write(src);
    }

    @Override
    public void close() throws IOException {
        socketChannel.close();
    }
}
