package com.just.networking.impl.tcp.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

import com.just.networking.Connection;
import com.just.networking.impl.tcp.TCPConnection;
import com.just.networking.server.ReadLoop;

public class TCPServer<C extends Connection<?>> implements AutoCloseable {

    private TCPServerConnection tcpServerConnection;

    private final TCPServerBindBroker tcpBindBroker;

    private final Function<TCPConnection, C> connectionPromotionFactory;

    public TCPServer(TCPServerBindBroker tcpBindBroker, Function<TCPConnection, C> connectionPromotionFactory) {
        this.tcpBindBroker = tcpBindBroker;
        this.connectionPromotionFactory = connectionPromotionFactory;
    }

    public <H> void start(
        String host,
        int port,
        ReadLoop<C, H> loop,
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
                            loop.run(connectionPromotionFactory.apply(connection), handlerSupplier.get());
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
