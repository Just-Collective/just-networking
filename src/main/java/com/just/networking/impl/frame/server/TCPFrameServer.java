package com.just.networking.impl.frame.server;

import com.bvanseg.just.functional.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.just.networking.config.frame.TCPFrameConfig;
import com.just.networking.impl.tcp.server.TCPServer;

public class TCPFrameServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCPFrameServer.class);

    private final TCPFrameConfig tcpFrameConfig;

    private final TCPServer tcpServer;

    public TCPFrameServer() {
        this(TCPFrameConfig.DEFAULT);
    }

    public TCPFrameServer(TCPFrameConfig tcpFrameConfig) {
        this(tcpFrameConfig, new TCPServer(tcpFrameConfig.tcp()));
    }

    public TCPFrameServer(TCPFrameConfig tcpFrameConfig, TCPServer tcpServer) {
        this.tcpFrameConfig = tcpFrameConfig;
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

        return result.map(connection -> new TCPFrameServerConnection(tcpFrameConfig, connection));
    }
}
