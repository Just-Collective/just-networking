package com.just.networking.impl.message.client;

import com.just.core.functional.result.Result;

import java.net.SocketAddress;

import com.just.networking.config.Config;
import com.just.networking.impl.frame.client.TCPFrameClient;
import com.just.networking.impl.message.MessageAccess;
import com.just.networking.impl.message.TCPMessageConnection;
import com.just.networking.impl.tcp.client.TCPClient;

public class TCPMessageClient {

    private final Config config;

    private final MessageAccess messageAccess;

    private final TCPFrameClient tcpFrameClient;

    public TCPMessageClient(
        Config config,
        MessageAccess messageAccess
    ) {
        this(config, messageAccess, new TCPFrameClient(config));
    }

    public TCPMessageClient(
        Config config,
        MessageAccess messageAccess,
        TCPFrameClient tcpFrameClient
    ) {
        this.config = config;
        this.messageAccess = messageAccess;
        this.tcpFrameClient = tcpFrameClient;
    }

    public Result<TCPMessageConnection, TCPClient.ConnectFailure<SocketAddress>> connect(
        String host,
        int port
    ) {
        var result = tcpFrameClient.connect(host, port);

        if (result.isOk()) {
            // Promote the connection to a TCPMessageConnection type.
            return Result.ok(new TCPMessageConnection(config, messageAccess, result.unwrap()));
        }

        return Result.err(result.unwrapErr());
    }
}
