package com.just.networking.impl.tcp.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import com.just.networking.Connection;
import com.just.networking.ConnectionBroker;
import com.just.networking.impl.tcp.TCPConnectionBroker;

public class TCPClient implements AutoCloseable {

    private volatile Connection connection;

    private final ConnectionBroker<SocketAddress> connectionBroker;

    public TCPClient() {
        this(new TCPConnectionBroker());
    }

    public TCPClient(ConnectionBroker<SocketAddress> broker) {
        this.connectionBroker = broker;
    }

    @Override
    public void close() {
        disconnect();
    }

    public ConnectionBroker.ConnectResult connect(String host, int port) {
        // Make sure to disconnect before attempting to make a new connection.
        disconnect();

        // Attempt to make a new connection.
        var result = connectionBroker.connect(new InetSocketAddress(host, port));

        if (result instanceof ConnectionBroker.ConnectResult.Connected(Connection successfulConnection)) {
            // if the connection attempt succeeded, store the connection.
            this.connection = successfulConnection;
        }

        // Bubble up the result to the caller.
        return result;
    }

    public int read(ByteBuffer dst) throws IOException {
        var c = connection;

        if (c == null) {
            return -1;
        }

        return c.transport().read(dst);
    }

    public int write(ByteBuffer src) throws IOException {
        var c = connection;

        if (c == null) {
            return -1;
        }

        return c.transport().write(src);
    }

    public void disconnect() {
        var c = connection;

        if (c == null || !c.isOpen()) {
            return;
        }

        try {
            c.close();
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
    }

    public boolean isOpen() {
        var c = connection;
        return c != null && c.isOpen();
    }
}
