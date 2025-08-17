package com.just.networking.impl.tcp.server;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;

import com.just.networking.server.ServerBindBroker;

public class TCPServerBindBroker implements ServerBindBroker<SocketAddress, TCPServerConnection> {

    @Override
    public TCPServerConnection bind(SocketAddress bindAddress) throws IOException {
        var ssc = ServerSocketChannel.open();
        ssc.configureBlocking(true);
        // Typical server options:
        ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        ssc.bind(bindAddress);

        return new TCPServerConnection(ssc);
    }
}
