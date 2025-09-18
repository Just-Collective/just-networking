package com.just.networking.impl.message.server;

import com.just.core.functional.result.Result;

import java.io.IOException;

import com.just.networking.config.Config;
import com.just.networking.impl.frame.server.TCPFrameServerConnection;
import com.just.networking.impl.message.MessageAccess;
import com.just.networking.impl.message.TCPMessageConnection;
import com.just.networking.server.ServerConnection;

public final class TCPMessageServerConnection implements ServerConnection<TCPMessageConnection> {

    private final Config config;

    private final MessageAccess messageAccess;

    private final TCPFrameServerConnection tcpFrameServerConnection;

    public TCPMessageServerConnection(
        Config config,
        MessageAccess messageAccess,
        TCPFrameServerConnection tcpFrameServerConnection
    ) {
        this.config = config;
        this.messageAccess = messageAccess;
        this.tcpFrameServerConnection = tcpFrameServerConnection;
    }

    @Override
    public boolean isOpen() {
        return tcpFrameServerConnection.isOpen();
    }

    @Override
    public Result<TCPMessageConnection, IOException> accept() {
        return tcpFrameServerConnection.accept()
            .map(connection -> new TCPMessageConnection(config, messageAccess, connection));
    }

    @Override
    public Result<Void, IOException> closeWithResult() {
        return tcpFrameServerConnection.closeWithResult();
    }

    public MessageAccess getMessageAccess() {
        return messageAccess;
    }
}
