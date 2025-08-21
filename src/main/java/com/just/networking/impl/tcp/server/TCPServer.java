package com.just.networking.impl.tcp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.NetworkChannel;
import java.nio.channels.ServerSocketChannel;

import com.just.networking.config.tcp.TCPConfig;

public class TCPServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCPServer.class);

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
        LOGGER.info("Opening server socket channel...");
        var serverSocketChannel = ServerSocketChannel.open();
        // Choose blocking semantics explicitly.
        serverSocketChannel.configureBlocking(true);
        // Common and safe default for servers: allow quick rebinding on restart.
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        LOGGER.info("Binding to {} ...", socketAddress);
        serverSocketChannel.bind(socketAddress);

        if (serverSocketChannel.isOpen()) {
            // Log where we actually bound and current blocking mode.
            var local = serverSocketChannel.getLocalAddress();
            LOGGER.info(
                "Successfully bound. Local address: {}, blocking: {}.",
                local,
                serverSocketChannel.isBlocking()
            );

            // Dump all supported socket options (name, type, value) at DEBUG level.
            debugDumpOptions(serverSocketChannel);
        } else {
            // Should be rare, but handle defensively.
            LOGGER.error("Failed to bind to {}: channel is not open after bind.", socketAddress);
        }

        return new TCPServerConnection(serverSocketChannel);
    }

    // List supported options and log current values at DEBUG level.
    private void debugDumpOptions(NetworkChannel networkChannel) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        try {
            var options = networkChannel.supportedOptions();

            // Header.
            LOGGER.debug("Server socket supported options ({}):", options.size());

            for (var opt : options) {
                // Try to read the value; some options can throw, so be defensive.
                Object value;
                try {
                    value = networkChannel.getOption(opt);
                } catch (UnsupportedOperationException e) {
                    LOGGER.debug("  {} ({}): <unsupported on this platform>", opt.name(), opt.type().getSimpleName());
                    continue;
                } catch (IOException e) {
                    LOGGER.debug(
                        "  {} ({}): <I/O error while reading: {}>",
                        opt.name(),
                        opt.type().getSimpleName(),
                        e.toString()
                    );
                    continue;
                } catch (SecurityException e) {
                    LOGGER.debug("  {} ({}): <denied by security manager>", opt.name(), opt.type().getSimpleName());
                    continue;
                }

                // Pretty-print the value.
                LOGGER.debug("  {} ({}): {}", opt.name(), opt.type().getSimpleName(), value);
            }
        } catch (Exception e) {
            // Last-resort guard so logging never interferes with server startup.
            LOGGER.debug("Failed to enumerate socket options: {}", e.toString());
        }
    }
}
