package com.just.networking.impl.tcp;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

import com.just.networking.ConnectionBroker;

public class TCPConnectionBroker implements ConnectionBroker<SocketAddress> {

    @Override
    public ConnectResult connect(SocketAddress socketAddress) {
        try {
            var socketChannel = SocketChannel.open();
            // pick blocking semantics explicitly.
            socketChannel.configureBlocking(true);
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

            // blocking connect - returns only when connected or throws.
            socketChannel.connect(socketAddress);

            return new ConnectResult.Connected(new TCPConnection(socketChannel));

        } catch (UnresolvedAddressException e) {
            return new ConnectResult.Refused("Unresolved address: " + socketAddress);
        } catch (ConnectException e) {
            return new ConnectResult.Refused(e.getMessage());
        } catch (IOException e) {
            return new ConnectResult.Failed(e);
        }
    }
}
