package com.just.networking.impl.tcp;

import com.bvanseg.just.functional.result.Result;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

import com.just.networking.ConnectionBroker;

public class TCPConnectionBroker implements ConnectionBroker<TCPConnection, SocketAddress> {

    @Override
    public Result<TCPConnection, Void> connect(SocketAddress socketAddress) {
        try {
            var socketChannel = SocketChannel.open();
            // pick blocking semantics explicitly.
            socketChannel.configureBlocking(true);
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

            // blocking connect - returns only when connected or throws.
            socketChannel.connect(socketAddress);

            return Result.ok(new TCPConnection(socketChannel));

            // TODO:
        } catch (UnresolvedAddressException e) {
            return Result.err(null);
        } catch (ConnectException e) {
            return Result.err(null);
        } catch (IOException e) {
            return Result.err(null);
        }
    }
}
