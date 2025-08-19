package com.just.networking.impl.tcp.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;

import com.just.networking.config.tcp.TCPConfig;

public class TCPServer {

    private final TCPConfig tcpConfig;

    public TCPServer() {
        this(TCPConfig.DEFAULT);
    }

    public TCPServer(TCPConfig tcpConfig) {
        this.tcpConfig = tcpConfig;
    }

    public TCPServerConnection bind(String host, int port) throws IOException {
        return bind(new InetSocketAddress(host, port));
    }

    public TCPServerConnection bind(SocketAddress socketAddress) throws IOException {
        var serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(true);
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverSocketChannel.bind(socketAddress);

        return new TCPServerConnection(serverSocketChannel);
    }
}
