package com.just.networking.impl.frame.client;

import com.bvanseg.just.functional.result.Result;

import com.just.networking.impl.frame.TCPFrameConnection;
import com.just.networking.impl.tcp.client.TCPClient;
import com.just.networking.impl.tcp.client.TCPClientConnectionBroker;

public final class TCPFrameClient {

    private final TCPClient tcpClient;

    public TCPFrameClient() {
        this(new TCPClient(new TCPClientConnectionBroker()));
    }

    public TCPFrameClient(TCPClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    public Result<TCPFrameConnection, Void> connect(String host, int port) {
        var result = tcpClient.connect(host, port);

        if (result.isOk()) {
            // Promote the connection to a TCPFrameConnection type.
            return Result.ok(new TCPFrameConnection(result.unwrap()));
        }

        return Result.err(result.unwrapErr());
    }
}
