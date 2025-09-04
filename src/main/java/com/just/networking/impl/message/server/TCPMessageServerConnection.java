package com.just.networking.impl.message.server;

import com.bvanseg.just.functional.result.Result;
import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.just.networking.config.Config;
import com.just.networking.impl.frame.server.TCPFrameServerConnection;
import com.just.networking.impl.message.Message;
import com.just.networking.impl.message.TCPMessageConnection;
import com.just.networking.server.ServerConnection;

public final class TCPMessageServerConnection implements ServerConnection<TCPMessageConnection> {

    private final Config config;

    private final StreamCodecSchema<ByteBuffer> schema;

    private final Map<Short, StreamCodec<? extends Message<?>>> streamCodecs;

    private final TCPFrameServerConnection tcpFrameServerConnection;

    public TCPMessageServerConnection(
        Config config,
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs,
        TCPFrameServerConnection tcpFrameServerConnection
    ) {
        this.config = config;
        this.schema = schema;
        this.streamCodecs = streamCodecs;
        this.tcpFrameServerConnection = tcpFrameServerConnection;
    }

    @Override
    public boolean isOpen() {
        return tcpFrameServerConnection.isOpen();
    }

    @Override
    public Result<TCPMessageConnection, IOException> accept() {
        return tcpFrameServerConnection.accept()
            .map(connection -> new TCPMessageConnection(config, schema, streamCodecs, connection));
    }

    @Override
    public Result<Void, IOException> closeWithResult() {
        return tcpFrameServerConnection.closeWithResult();
    }
}
