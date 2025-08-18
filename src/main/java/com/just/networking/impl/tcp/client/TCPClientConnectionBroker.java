package com.just.networking.impl.tcp.client;

import com.bvanseg.just.functional.result.Result;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

import com.just.networking.ClientConnectionBroker;
import com.just.networking.impl.tcp.TCPConnection;

public class TCPClientConnectionBroker implements ClientConnectionBroker<TCPConnection, SocketAddress, TCPClientConnectionBroker.ConnectFailure<SocketAddress>> {

    @Override
    public Result<TCPConnection, ConnectFailure<SocketAddress>> connect(SocketAddress socketAddress) {
        try {
            var socketChannel = SocketChannel.open();
            // pick blocking semantics explicitly.
            socketChannel.configureBlocking(true);
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

            // blocking connect - returns only when connected or throws.
            socketChannel.connect(socketAddress);

            return Result.ok(new TCPConnection(socketChannel));

        } catch (UnresolvedAddressException e) {
            return Result.err(new ConnectFailure.UnresolvedAddress<>(socketAddress, e));
        } catch (ConnectException e) {
            return Result.err(new ConnectFailure.ConnectionRefused<>(socketAddress, e));
        } catch (IOException e) {
            return Result.err(new ConnectFailure.IoFailure<>(socketAddress, e));
        }
    }

    public sealed interface ConnectFailure<Target> {

        String reason();

        Throwable cause();

        Target target();

        // Address could not be resolved (bad hostname, DNS issue, etc.).
        record UnresolvedAddress<Target>(
            Target target,
            UnresolvedAddressException cause
        ) implements ConnectFailure<Target> {

            @Override
            public String reason() {
                return "Unresolved address.";
            }
        }

        // TCP connection attempt failed (refused, unreachable, timeout as ConnectException).
        record ConnectionRefused<Target>(
            Target target,
            ConnectException cause
        ) implements ConnectFailure<Target> {

            @Override
            public String reason() {
                return "Connection refused.";
            }
        }

        // Any other IOException during open/configure/connect.
        record IoFailure<Target>(
            Target target,
            IOException cause
        ) implements ConnectFailure<Target> {

            @Override
            public String reason() {
                return "I/O failure during connect.";
            }
        }
    }
}
