package com.just.networking.impl.message.server;

import com.just.core.functional.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.just.networking.config.Config;
import com.just.networking.impl.frame.server.TCPFrameServer;
import com.just.networking.impl.message.MessageAccess;
import com.just.networking.impl.tcp.server.TCPServer;

public class TCPMessageServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCPMessageServer.class);

    private final Config config;

    private final MessageAccess messageAccess;

    private final TCPFrameServer tcpFrameServer;

    public TCPMessageServer(
        Config config,
        MessageAccess messageAccess
    ) {
        this(config, messageAccess, new TCPFrameServer(config));
    }

    public TCPMessageServer(
        Config config,
        MessageAccess messageAccess,
        TCPFrameServer tcpFrameServer
    ) {
        this.config = config;
        this.messageAccess = messageAccess;
        this.tcpFrameServer = tcpFrameServer;
    }

    public Result<TCPMessageServerConnection, TCPServer.BindFailure<SocketAddress>> bind(
        String host,
        int port
    ) {
        return bind(new InetSocketAddress(host, port));
    }

    public Result<TCPMessageServerConnection, TCPServer.BindFailure<SocketAddress>> bind(
        SocketAddress bindAddress
    ) {
        var result = tcpFrameServer.bind(bindAddress);

        // Announce the upgrade from raw TCP to message transport.
        result.ifOk(
            $ -> LOGGER.info(
                "Upgraded TCP Frame server at {} to message transport (TCPMessageServerConnection).",
                bindAddress
            )
        );

        return result.map(
            connection -> new TCPMessageServerConnection(config, messageAccess, connection)
        );
    }

    public MessageAccess getMessageAccess() {
        return messageAccess;
    }
}
