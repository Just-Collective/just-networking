package com.just.networking.impl.frame.client;

import com.bvanseg.just.functional.result.Result;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.just.networking.impl.frame.TCPFrameConnection;
import com.just.networking.impl.tcp.TCPConnectionBroker;
import com.just.networking.impl.tcp.client.TCPClient;

public final class TCPFrameClient implements AutoCloseable {

    private volatile TCPFrameConnection tcpFrameConnection;

    private final TCPClient tcpClient;

    public TCPFrameClient() {
        this(new TCPClient(new TCPConnectionBroker()));
    }

    public TCPFrameClient(TCPClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    @Override
    public void close() {
        disconnect();
    }

    public Result<TCPFrameConnection, Void> connect(String host, int port) {
        var result = tcpClient.connect(host, port);

        if (result.isOk()) {
            // Promote the connection to a TCPFrameConnection type.
            this.tcpFrameConnection = new TCPFrameConnection(result.unwrap());
            return Result.ok(tcpFrameConnection);
        }

        return Result.err(result.unwrapErr());
    }

    public ByteBuffer readFrame() throws IOException {
        // TODO: Null check.
        return tcpFrameConnection.transport().readFrame();
    }

    public void sendFrame(ByteBuffer payload) throws IOException {
        // TODO: Null check.
        tcpFrameConnection.transport().sendFrame(payload);
    }

    public void flushWrites() throws IOException {
        // TODO: Null check.
        tcpFrameConnection.transport().flushWrites();
    }

    public void disconnect() {
        tcpClient.disconnect();

        // TODO: Null check.
        try {
            tcpFrameConnection.transport().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isOpen() {
        return tcpClient.isOpen();
    }
}
