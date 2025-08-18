package com.just.networking.impl.frame.server;

import com.just.networking.impl.frame.TCPFrameConnection;
import com.just.networking.impl.tcp.server.TCPServer;
import com.just.networking.impl.tcp.server.TCPServerBindBroker;

public class TCPFrameServer extends TCPServer<TCPFrameConnection> {

    public TCPFrameServer() {
        super(new TCPServerBindBroker(), TCPFrameConnection::new);
    }
}
