package com.just.networking.impl.frame;

import com.just.core.functional.result.Result;

import java.io.IOException;

import com.just.networking.Connection;
import com.just.networking.config.Config;
import com.just.networking.impl.tcp.TCPConnection;

public class TCPFrameConnection implements Connection<TCPFrameTransport> {

    private final TCPConnection tcpConnection;

    private final TCPFrameTransport tcpFrameTransport;

    public TCPFrameConnection(Config config, TCPConnection tcpConnection) {
        this.tcpConnection = tcpConnection;
        this.tcpFrameTransport = new TCPFrameTransport(config, tcpConnection.transport());
    }

    @Override
    public TCPFrameTransport transport() {
        return tcpFrameTransport;
    }

    @Override
    public Result<Void, IOException> closeWithResult() {
        return tcpConnection.closeWithResult();
    }
}
