package com.just.networking.impl.tcp;

import com.just.core.functional.result.Result;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.just.networking.Transport;
import com.just.networking.Writer;

public class TCPTransport implements Transport, Writer {

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

    @Override
    public int write(ByteBuffer src) throws IOException {
        return socketChannel.write(src);
    }

    @Override
    public long write(ByteBuffer[] srcs) throws IOException {
        return socketChannel.write(srcs);
    }

    @Override
    public Result<Void, IOException> closeWithResult() {
        return Result.tryRun(socketChannel::close);
    }
}
