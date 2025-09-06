package com.just.networking.impl.message;

import com.just.core.functional.result.Result;

import java.io.IOException;

import com.just.networking.Connection;
import com.just.networking.config.Config;
import com.just.networking.impl.frame.TCPFrameConnection;

public class TCPMessageConnection implements Connection<TCPMessageTransport> {

    private final TCPFrameConnection tcpFrameConnection;

    private final TCPMessageTransport tcpMessageTransport;

    public TCPMessageConnection(
        Config config,
        MessageAccess messageAccess,
        TCPFrameConnection tcpFrameConnection
    ) {
        this.tcpFrameConnection = tcpFrameConnection;
        this.tcpMessageTransport = new TCPMessageTransport(config, messageAccess, tcpFrameConnection.transport());
    }

    @Override
    public TCPMessageTransport transport() {
        return tcpMessageTransport;
    }

    @Override
    public Result<Void, IOException> closeWithResult() {
        return tcpFrameConnection.closeWithResult();
    }

    public TCPFrameConnection asTCPFrameConnection() {
        return tcpFrameConnection;
    }
}
