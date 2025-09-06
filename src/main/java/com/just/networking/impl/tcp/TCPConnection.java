package com.just.networking.impl.tcp;

import com.just.core.functional.result.Result;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import com.just.networking.Connection;

public class TCPConnection implements Connection<TCPTransport> {

    private final TCPTransport transport;

    public TCPConnection(SocketChannel socketChannel) {
        this.transport = new TCPTransport(socketChannel);
    }

    @Override
    public TCPTransport transport() {
        return transport;
    }

    @Override
    public Result<Void, IOException> closeWithResult() {
        return transport.closeWithResult();
    }
}
