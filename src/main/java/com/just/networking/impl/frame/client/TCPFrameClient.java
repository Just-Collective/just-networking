package com.just.networking.impl.frame.client;

import com.bvanseg.just.functional.result.Result;

import java.net.SocketAddress;

import com.just.networking.config.frame.TCPFrameConfig;
import com.just.networking.impl.frame.TCPFrameConnection;
import com.just.networking.impl.tcp.client.TCPClient;

public final class TCPFrameClient {

    private final TCPFrameConfig tcpFrameConfig;

    private final TCPClient tcpClient;

    public TCPFrameClient(TCPFrameConfig tcpFrameConfig) {
        this(tcpFrameConfig, new TCPClient(tcpFrameConfig.tcp()));
    }

    public TCPFrameClient(TCPFrameConfig tcpFrameConfig, TCPClient tcpClient) {
        this.tcpFrameConfig = tcpFrameConfig;
        this.tcpClient = tcpClient;
    }

    public Result<TCPFrameConnection, TCPClient.ConnectFailure<SocketAddress>> connect(
        String host,
        int port
    ) {
        var result = tcpClient.connect(host, port);

        if (result.isOk()) {
            // Promote the connection to a TCPFrameConnection type.
            return Result.ok(new TCPFrameConnection(tcpFrameConfig, result.unwrap()));
        }

        return Result.err(result.unwrapErr());
    }
}
