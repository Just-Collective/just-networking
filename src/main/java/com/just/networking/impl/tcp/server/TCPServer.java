package com.just.networking.impl.tcp.server;

import com.just.core.functional.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.NetworkChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.UnresolvedAddressException;

import com.just.networking.config.Config;
import com.just.networking.config.DefaultConfigKeys;

public class TCPServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCPServer.class);

    private final Config config;

    public TCPServer(Config config) {
        this.config = config;
    }

    public Result<TCPServerConnection, BindFailure<SocketAddress>> bind(String host, int port) {
        return bind(new InetSocketAddress(host, port));
    }

    public Result<TCPServerConnection, BindFailure<SocketAddress>> bind(SocketAddress socketAddress) {
        LOGGER.info("Opening server socket channel...");

        ServerSocketChannel serverSocketChannel;
        try {
            serverSocketChannel = ServerSocketChannel.open();
        } catch (IOException | SecurityException e) {
            LOGGER.error("Failed to open server socket channel.", e);
            return Result.err(new BindFailure.OpenFailed<>(socketAddress, e));
        }

        try {
            // Choose blocking semantics explicitly.
            var blocking = config.get(DefaultConfigKeys.TCP_SERVER_SOCKET_BLOCKING);
            serverSocketChannel.configureBlocking(blocking);
        } catch (IOException e) {
            LOGGER.error("Failed to configure blocking mode.", e);
            closeQuietly(serverSocketChannel);
            return Result.err(new BindFailure.ConfigureBlockingFailed<>(socketAddress, e));
        }

        var soReuseAddress = config.get(DefaultConfigKeys.TCP_SERVER_SOCKET_SO_REUSEADDR);
        try {
            // Common and safe default for servers: allow quick rebinding on restart.
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, soReuseAddress);
        } catch (UnsupportedOperationException e) {
            // Not fatal; just log at DEBUG since some platforms may not support it.
            LOGGER.debug("SO_REUSEADDR unsupported on this platform.");
        } catch (IOException | SecurityException e) {
            LOGGER.error("Failed to set socket option SO_REUSEADDR.", e);
            closeQuietly(serverSocketChannel);
            return Result.err(
                new BindFailure.SetOptionFailed<>(socketAddress, StandardSocketOptions.SO_REUSEADDR, soReuseAddress, e)
            );
        }

        LOGGER.info("Binding to {} ...", socketAddress);
        try {
            serverSocketChannel.bind(socketAddress);
        } catch (UnresolvedAddressException e) {
            LOGGER.error("Bind failed: unresolved address {}.", socketAddress, e);
            closeQuietly(serverSocketChannel);
            return Result.err(new BindFailure.AddressUnresolved<>(socketAddress, e));
        } catch (AlreadyBoundException e) {
            LOGGER.error("Bind failed: channel already bound.", e);
            closeQuietly(serverSocketChannel);
            return Result.err(new BindFailure.AlreadyBound<>(socketAddress, e));
        } catch (SecurityException e) {
            LOGGER.error("Bind failed: permission denied for {}.", socketAddress, e);
            closeQuietly(serverSocketChannel);
            return Result.err(new BindFailure.PermissionDenied<>(socketAddress, e));
        } catch (IOException e) {
            // Port in use typically surfaces as a BindException (subclass of IOException).
            if (e instanceof BindException) {
                LOGGER.error("Bind failed: address already in use: {}.", socketAddress, e);
                closeQuietly(serverSocketChannel);
                return Result.err(new BindFailure.PortAlreadyInUse<>(socketAddress, e));
            }

            LOGGER.error("Bind failed with I/O error for {}.", socketAddress, e);
            closeQuietly(serverSocketChannel);
            return Result.err(new BindFailure.BindIOFailed<>(socketAddress, e));
        }

        if (!serverSocketChannel.isOpen()) {
            LOGGER.error("Failed to bind to {}: channel not open after bind.", socketAddress);
            closeQuietly(serverSocketChannel);
            return Result.err(new BindFailure.NotOpenAfterBind<>(socketAddress));
        }

        try {
            var local = serverSocketChannel.getLocalAddress();
            LOGGER.info(
                "Successfully bound. Local address: {}, blocking: {}.",
                local,
                serverSocketChannel.isBlocking()
            );
        } catch (IOException e) {
            // Non-fatal: we are bound, but couldn't resolve local address for logging.
            LOGGER.debug("Bound, but failed to resolve local address for logging: {}.", e.toString());
        }

        // Dump all supported socket options (name, type, value) at DEBUG level.
        debugDumpOptions(serverSocketChannel);

        return Result.ok(new TCPServerConnection(config, serverSocketChannel));
    }

    private static void closeQuietly(ServerSocketChannel serverSocketChannel) {
        try {
            serverSocketChannel.close();
        } catch (Exception ignored) {}
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

    public sealed interface BindFailure<T> {

        String reason();

        Throwable cause();

        T target();

        // Opening the channel failed.
        record OpenFailed<T>(
            T target,
            Throwable cause
        ) implements BindFailure<T> {

            @Override
            public String reason() {
                return "Failed to open server channel.";
            }
        }

        // Configuring blocking mode failed.
        record ConfigureBlockingFailed<T>(
            T target,
            Throwable cause
        ) implements BindFailure<T> {

            @Override
            public String reason() {
                return "Failed to configure blocking mode.";
            }
        }

        // Setting a socket option failed (fatal variant).
        record SetOptionFailed<T>(
            T target,
            SocketOption<?> option,
            Object attemptedValue,
            Throwable cause
        ) implements BindFailure<T> {

            @Override
            public String reason() {
                return "Failed to set socket option: " + option.name() + ".";
            }
        }

        // Provided address could not be resolved.
        record AddressUnresolved<T>(
            T target,
            Throwable cause
        ) implements BindFailure<T> {

            @Override
            public String reason() {
                return "Unresolved bind address.";
            }
        }

        // The channel was already bound (shouldn't happen for a fresh channel).
        record AlreadyBound<T>(
            T target,
            Throwable cause
        ) implements BindFailure<T> {

            @Override
            public String reason() {
                return "Channel already bound.";
            }
        }

        // Security manager or policy denied the bind.
        record PermissionDenied<T>(
            T target,
            Throwable cause
        ) implements BindFailure<T> {

            @Override
            public String reason() {
                return "Permission denied while binding.";
            }
        }

        // Typical "Address already in use" case.
        record PortAlreadyInUse<T>(
            T target,
            Throwable cause
        ) implements BindFailure<T> {

            @Override
            public String reason() {
                return "Address already in use.";
            }
        }

        // Other I/O error during bind.
        record BindIOFailed<T>(
            T target,
            Throwable cause
        ) implements BindFailure<T> {

            @Override
            public String reason() {
                return "I/O error during bind.";
            }
        }

        // Postcondition: channel not open after bind.
        record NotOpenAfterBind<T>(T target) implements BindFailure<T> {

            @Override
            public String reason() {
                return "Channel not open after bind.";
            }

            @Override
            public Throwable cause() {
                return null;
            }
        }

    }
}
