package com.just.networking.impl.tcp.client;

import com.just.core.functional.result.Result;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

import com.just.networking.config.Config;
import com.just.networking.config.DefaultConfigKeys;
import com.just.networking.impl.tcp.TCPConnection;

public class TCPClient {

    private final Config config;

    public TCPClient(Config config) {
        this.config = config;
    }

    public Result<TCPConnection, ConnectFailure<SocketAddress>> connect(
        String host,
        int port
    ) {
        var socketAddress = new InetSocketAddress(host, port);

        try {
            var socketChannel = SocketChannel.open();

            // Configure blocking vs. non-blocking for a connection attempt (see timeout below).
            var blocking = config.get(DefaultConfigKeys.TCP_CLIENT_SOCKET_BLOCKING);
            var connectTimeoutMillis = config.get(DefaultConfigKeys.TCP_CLIENT_SOCKET_CONNECT_TIMEOUT_MILLIS);
            var needsTimeout = connectTimeoutMillis > 0;
            // non-blocking if we need to enforce timeout.
            socketChannel.configureBlocking(!needsTimeout);

            socketChannel.setOption(
                StandardSocketOptions.TCP_NODELAY,
                config.get(DefaultConfigKeys.TCP_CLIENT_SOCKET_NO_DELAY)
            );
            socketChannel.setOption(
                StandardSocketOptions.SO_KEEPALIVE,
                config.get(DefaultConfigKeys.TCP_CLIENT_SOCKET_KEEP_ALIVE)
            );
            config.get(DefaultConfigKeys.TCP_CLIENT_SOCKET_SO_LINGER_SECONDS).ifSome(soLingerSeconds -> {
                try {
                    socketChannel.setOption(StandardSocketOptions.SO_LINGER, soLingerSeconds);
                } catch (IOException ignored) {
                    // TODO: Log a warning.
                }
            });

            // Simple path: blocking connect
            socketChannel.connect(socketAddress);

            if (!needsTimeout) {
                socketChannel.configureBlocking(blocking);
            } else {
                try (var sel = Selector.open()) {
                    socketChannel.register(sel, SelectionKey.OP_CONNECT);

                    if (sel.select(connectTimeoutMillis) == 0) {
                        socketChannel.close();
                        return Result.err(
                            new ConnectFailure.ConnectionRefused<>(
                                socketAddress,
                                new ConnectException("Connect timeout after " + connectTimeoutMillis + "ms.")
                            )
                        );
                    }

                    for (var key : sel.selectedKeys()) {
                        if (key.isConnectable()) {
                            var keySocketChannel = (SocketChannel) key.channel();

                            if (!keySocketChannel.finishConnect()) {
                                keySocketChannel.close();
                                return Result.err(
                                    new ConnectFailure.ConnectionRefused<>(
                                        socketAddress,
                                        new ConnectException("finishConnect() failed")
                                    )
                                );
                            }
                        }
                    }
                }

                // Restore desired runtime blocking mode.
                socketChannel.configureBlocking(blocking);
            }

            return Result.ok(new TCPConnection(socketChannel));

        } catch (UnresolvedAddressException e) {
            return Result.err(new ConnectFailure.UnresolvedAddress<>(socketAddress, e));
        } catch (ConnectException e) {
            return Result.err(new ConnectFailure.ConnectionRefused<>(socketAddress, e));
        } catch (IOException e) {
            return Result.err(new ConnectFailure.IoFailure<>(socketAddress, e));
        }
    }

    public sealed interface ConnectFailure<T> {

        String reason();

        Throwable cause();

        T target();

        // Address could not be resolved (bad hostname, DNS issue, etc.).
        record UnresolvedAddress<T>(
            T target,
            UnresolvedAddressException cause
        ) implements ConnectFailure<T> {

            @Override
            public String reason() {
                return "Unresolved address.";
            }
        }

        // TCP connection attempt failed (refused, unreachable, timeout as ConnectException).
        record ConnectionRefused<T>(
            T target,
            ConnectException cause
        ) implements ConnectFailure<T> {

            @Override
            public String reason() {
                return "Connection refused.";
            }
        }

        // Any other IOException during open/configure/connect.
        record IoFailure<T>(
            T target,
            IOException cause
        ) implements ConnectFailure<T> {

            @Override
            public String reason() {
                return "I/O failure during connect.";
            }
        }
    }
}
