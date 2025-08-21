package com.just.networking.impl.tcp.server;

import com.bvanseg.just.functional.result.Result;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.util.function.Supplier;

import com.just.networking.impl.tcp.TCPConnection;
import com.just.networking.server.ServerConnection;

public final class TCPServerConnection implements ServerConnection<TCPConnection> {

    private final ServerSocketChannel serverSocketChannel;

    public TCPServerConnection(ServerSocketChannel serverSocketChannel) {
        this.serverSocketChannel = serverSocketChannel;
    }

    @Override
    public boolean isOpen() {
        return serverSocketChannel.isOpen();
    }

    @Override
    public TCPConnection accept() throws IOException {
        if (!serverSocketChannel.isOpen()) {
            throw new IllegalStateException("Server not bound. Call bind() first.");
        }

        // Blocking accept.
        var socketChannel = serverSocketChannel.accept();

        // Set per-connection options; channel starts connected
        socketChannel.configureBlocking(true);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

        return new TCPConnection(socketChannel);
    }

    @Override
    public Result<Void, IOException> closeWithResult() {
        return Result.tryRun(serverSocketChannel::close);
    }

    public <H extends ByteReadLoopHandler<TCPConnection>> Thread listen(Supplier<? extends H> handlerSupplier) {
        return listen(new TCPByteReadLoop<>(), handlerSupplier);
    }
}
