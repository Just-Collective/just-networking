package com.just.networking.impl.tcp.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.just.networking.impl.tcp.TCPConnection;
import com.just.networking.server.ReadLoop;

public final class TCPServer implements AutoCloseable {

    private TCPServerConnection tcpServerConnection;

    private final TCPServerBindBroker tcpBindBroker;

    public TCPServer() {
        this(new TCPServerBindBroker());
    }

    public TCPServer(TCPServerBindBroker tcpBindBroker) {
        this.tcpBindBroker = tcpBindBroker;
    }

    public <H> void start(
        String host,
        int port,
        ReadLoop<TCPConnection, H> loop,
        Supplier<? extends H> handlerSupplier
    ) throws IOException {
        this.tcpServerConnection = tcpBindBroker.bind(new InetSocketAddress(host, port));

        Thread.ofPlatform().name("tcp-accept").start(() -> {
            // Use virtual threads -> clean blocking code + scalable
            var executorService = Executors.newVirtualThreadPerTaskExecutor();

            try (executorService) {
                while (tcpServerConnection != null && tcpServerConnection.isOpen()) {
                    var connection = tcpServerConnection.accept();

                    executorService.submit(() -> {
                        try {
                            loop.run(connection, handlerSupplier.get());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void close() throws IOException {
        var tcpsc = tcpServerConnection;

        if (tcpsc != null) {
            if (tcpsc.isOpen()) {
                tcpsc.close();
            }

            this.tcpServerConnection = null;
        }
    }
}
