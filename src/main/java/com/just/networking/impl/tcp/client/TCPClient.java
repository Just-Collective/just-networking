package com.just.networking.impl.tcp.client;

import com.bvanseg.just.functional.result.Result;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.just.networking.ClientConnectionBroker;
import com.just.networking.impl.tcp.TCPConnection;

public class TCPClient {

    private final ClientConnectionBroker<TCPConnection, SocketAddress, TCPClientConnectionBroker.ConnectFailure<SocketAddress>> clientConnectionBroker;

    public TCPClient() {
        this(new TCPClientConnectionBroker());
    }

    public TCPClient(TCPClientConnectionBroker broker) {
        this.clientConnectionBroker = broker;
    }

    public Result<TCPConnection, TCPClientConnectionBroker.ConnectFailure<SocketAddress>> connect(
        String host,
        int port
    ) {
        return clientConnectionBroker.connect(new InetSocketAddress(host, port));
    }
}
