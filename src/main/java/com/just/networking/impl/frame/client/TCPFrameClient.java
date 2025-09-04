package com.just.networking.impl.frame.client;

import com.bvanseg.just.functional.result.Result;

import java.net.SocketAddress;

import com.just.networking.config.Config;
import com.just.networking.impl.frame.TCPFrameConnection;
import com.just.networking.impl.tcp.client.TCPClient;

public final class TCPFrameClient {

    private final Config config;

    private final TCPClient tcpClient;

    public TCPFrameClient(Config config) {
        this(config, new TCPClient(config));
    }

    public TCPFrameClient(Config config, TCPClient tcpClient) {
        this.config = config;
        this.tcpClient = tcpClient;
    }

    public Result<TCPFrameConnection, TCPClient.ConnectFailure<SocketAddress>> connect(
        String host,
        int port
    ) {
        var result = tcpClient.connect(host, port);

        if (result.isOk()) {
            // Promote the connection to a TCPFrameConnection type.
            return Result.ok(new TCPFrameConnection(config, result.unwrap()));
        }

        return Result.err(result.unwrapErr());
    }
}
