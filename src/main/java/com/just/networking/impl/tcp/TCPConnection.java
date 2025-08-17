package com.just.networking.impl.tcp;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import com.just.networking.Connection;
import com.just.networking.Transport;

public class TCPConnection implements Connection {

    private final Transport transport;

    public TCPConnection(SocketChannel socketChannel) {
        this.transport = new TCPTransport(socketChannel);
    }

    @Override
    public Transport transport() {
        return transport;
    }

    @Override
    public void close() throws IOException {
        transport.close();
    }
}
