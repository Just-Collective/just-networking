package com.just.networking.impl.frame.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.just.networking.config.frame.TCPFrameConfig;
import com.just.networking.impl.tcp.server.TCPServer;

public class TCPFrameServer {

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

    public TCPFrameServerConnection bind(String host, int port) throws IOException {
        return bind(new InetSocketAddress(host, port));
    }

    public TCPFrameServerConnection bind(SocketAddress bindAddress) throws IOException {
        return new TCPFrameServerConnection(tcpFrameConfig, tcpServer.bind(bindAddress));
    }
}
