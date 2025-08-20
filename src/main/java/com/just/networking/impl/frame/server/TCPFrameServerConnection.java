package com.just.networking.impl.frame.server;

import java.io.IOException;
import java.util.function.Supplier;

import com.just.networking.config.frame.TCPFrameConfig;
import com.just.networking.impl.frame.TCPFrameConnection;
import com.just.networking.impl.tcp.server.TCPServerConnection;
import com.just.networking.server.ServerConnection;

public final class TCPFrameServerConnection implements ServerConnection<TCPFrameConnection> {

    private final TCPFrameConfig tcpFrameConfig;

    private final TCPServerConnection tcpServerConnection;

    public TCPFrameServerConnection(TCPFrameConfig tcpFrameConfig, TCPServerConnection tcpServerConnection) {
        this.tcpFrameConfig = tcpFrameConfig;
        this.tcpServerConnection = tcpServerConnection;
    }

    @Override
    public boolean isOpen() {
        return tcpServerConnection.isOpen();
    }

    @Override
    public TCPFrameConnection accept() throws IOException {
        return new TCPFrameConnection(tcpFrameConfig, tcpServerConnection.accept());
    }

    @Override
    public void close() throws IOException {
        tcpServerConnection.close();
    }

    public <H extends FrameReadLoopHandler<TCPFrameConnection>> Thread listen(Supplier<? extends H> handlerSupplier) {
        return listen(new TCPFrameReadLoop<>(), handlerSupplier);
    }
}
