package com.just.networking.impl.frame.server;

import com.just.core.functional.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.just.networking.config.Config;
import com.just.networking.impl.tcp.server.TCPServer;

public class TCPFrameServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCPFrameServer.class);

    private final Config config;

    private final TCPServer tcpServer;

    public TCPFrameServer(Config config) {
        this(config, new TCPServer(config));
    }

    public TCPFrameServer(Config config, TCPServer tcpServer) {
        this.config = config;
        this.tcpServer = tcpServer;
    }

    public Result<TCPFrameServerConnection, TCPServer.BindFailure<SocketAddress>> bind(
        String host,
        int port
    ) {
        return bind(new InetSocketAddress(host, port));
    }

    public Result<TCPFrameServerConnection, TCPServer.BindFailure<SocketAddress>> bind(
        SocketAddress bindAddress
    ) {
        var result = tcpServer.bind(bindAddress);

        // Announce the upgrade from raw TCP to framed transport.
        result.ifOk(
            $ -> LOGGER.info("Upgraded TCP server at {} to framed transport (TCPFrameServerConnection).", bindAddress)
        );

        return result.map(connection -> new TCPFrameServerConnection(config, connection));
    }
}
