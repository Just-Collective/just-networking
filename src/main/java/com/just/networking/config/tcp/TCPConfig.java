package com.just.networking.config.tcp;

import java.util.function.UnaryOperator;

public record TCPConfig(
    SocketConfig socket,
    ConnectionConfig connection,
    BufferConfig buffer
) {

    public static final TCPConfig DEFAULT = new TCPConfig(
        SocketConfig.DEFAULT,
        ConnectionConfig.DEFAULT,
        BufferConfig.DEFAULT
    );

    public TCPConfig withBufferConfig(UnaryOperator<BufferConfig> unaryOperator) {
        return new TCPConfig(socket, connection, unaryOperator.apply(buffer));
    }

    public TCPConfig withConnectionConfig(UnaryOperator<ConnectionConfig> unaryOperator) {
        return new TCPConfig(socket, unaryOperator.apply(connection), buffer);
    }

    public TCPConfig withSocketConfig(UnaryOperator<SocketConfig> unaryOperator) {
        return new TCPConfig(unaryOperator.apply(socket), connection, buffer);
    }
}
