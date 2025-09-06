package com.just.networking.impl.tcp.server;

import com.just.core.functional.result.Result;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.util.function.Supplier;

import com.just.networking.config.Config;
import com.just.networking.config.DefaultConfigKeys;
import com.just.networking.impl.tcp.TCPConnection;
import com.just.networking.server.ServerConnection;

public final class TCPServerConnection implements ServerConnection<TCPConnection> {

    private final Config config;

    private final ServerSocketChannel serverSocketChannel;

    public TCPServerConnection(Config config, ServerSocketChannel serverSocketChannel) {
        this.config = config;
        this.serverSocketChannel = serverSocketChannel;
    }

    @Override
    public boolean isOpen() {
        return serverSocketChannel.isOpen();
    }

    @Override
    public Result<TCPConnection, IOException> accept() {
        if (!serverSocketChannel.isOpen()) {
            throw new IllegalStateException("Server not bound. Call bind() first.");
        }

        // TODO: Return specific error type here instead of the exception type.
        try {
            // Blocking accept.
            var socketChannel = serverSocketChannel.accept();

            // Set per-connection options; channel starts connected
            socketChannel.configureBlocking(config.get(DefaultConfigKeys.TCP_SERVER_CLIENT_SOCKET_BLOCKING));
            socketChannel.setOption(
                StandardSocketOptions.TCP_NODELAY,
                config.get(DefaultConfigKeys.TCP_SERVER_CLIENT_SOCKET_NO_DELAY)
            );
            socketChannel.setOption(
                StandardSocketOptions.SO_KEEPALIVE,
                config.get(DefaultConfigKeys.TCP_SERVER_CLIENT_SOCKET_KEEP_ALIVE)
            );

            return Result.ok(new TCPConnection(socketChannel));
        } catch (IOException e) {
            return Result.err(e);
        }
    }

    @Override
    public Result<Void, IOException> closeWithResult() {
        return Result.tryRun(serverSocketChannel::close);
    }

    public <H extends ByteReadLoopHandler<TCPConnection>> Thread listen(Supplier<? extends H> handlerSupplier) {
        return listen(new TCPByteReadLoop<>(config), handlerSupplier);
    }
}
