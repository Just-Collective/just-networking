package com.just.networking.impl.message.server;

import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.just.networking.config.message.TCPMessageConfig;
import com.just.networking.impl.frame.server.TCPFrameServerConnection;
import com.just.networking.impl.message.Message;
import com.just.networking.impl.message.TCPMessageConnection;
import com.just.networking.server.ServerConnection;

public final class TCPMessageServerConnection implements ServerConnection<TCPMessageConnection> {

    private final TCPMessageConfig tcpMessageConfig;

    private final StreamCodecSchema<ByteBuffer> schema;

    private final Map<Short, StreamCodec<? extends Message<?>>> streamCodecs;

    private final TCPFrameServerConnection tcpFrameServerConnection;

    public TCPMessageServerConnection(
        TCPMessageConfig tcpMessageConfig,
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs,
        TCPFrameServerConnection tcpFrameServerConnection
    ) {
        this.tcpMessageConfig = tcpMessageConfig;
        this.schema = schema;
        this.streamCodecs = streamCodecs;
        this.tcpFrameServerConnection = tcpFrameServerConnection;
    }

    @Override
    public boolean isOpen() {
        return tcpFrameServerConnection.isOpen();
    }

    @Override
    public TCPMessageConnection accept() throws IOException {
        return new TCPMessageConnection(tcpMessageConfig, schema, streamCodecs, tcpFrameServerConnection.accept());
    }

    @Override
    public void close() throws IOException {
        tcpFrameServerConnection.close();
    }
}
