package com.just.networking.impl.message;

import com.bvanseg.just.functional.result.Result;
import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.just.networking.Connection;
import com.just.networking.config.Config;
import com.just.networking.impl.frame.TCPFrameConnection;

public class TCPMessageConnection implements Connection<TCPMessageTransport> {

    private final TCPFrameConnection tcpFrameConnection;

    private final TCPMessageTransport tcpMessageTransport;

    public TCPMessageConnection(
        Config config,
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs,
        TCPFrameConnection tcpFrameConnection
    ) {
        this.tcpFrameConnection = tcpFrameConnection;
        this.tcpMessageTransport = new TCPMessageTransport(
            config,
            schema,
            streamCodecs,
            tcpFrameConnection.transport()
        );
    }

    @Override
    public TCPMessageTransport transport() {
        return tcpMessageTransport;
    }

    @Override
    public Result<Void, IOException> closeWithResult() {
        return tcpFrameConnection.closeWithResult();
    }

    public TCPFrameConnection asTCPFrameConnection() {
        return tcpFrameConnection;
    }
}
