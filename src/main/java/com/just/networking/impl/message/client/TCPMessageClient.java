package com.just.networking.impl.message.client;

import com.bvanseg.just.functional.result.Result;
import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

import com.just.networking.impl.frame.client.TCPFrameClient;
import com.just.networking.impl.message.Message;
import com.just.networking.impl.message.TCPMessageConnection;
import com.just.networking.impl.tcp.client.TCPClientConnectionBroker;

public class TCPMessageClient {

    private final Map<Short, StreamCodec<? extends Message<?>>> streamCodecs;

    private final StreamCodecSchema<ByteBuffer> schema;

    private final TCPFrameClient tcpFrameClient;

    public TCPMessageClient(
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs
    ) {
        this(schema, streamCodecs, new TCPFrameClient());
    }

    public TCPMessageClient(
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs,
        TCPFrameClient tcpFrameClient
    ) {
        this.streamCodecs = streamCodecs;
        this.schema = schema;
        this.tcpFrameClient = tcpFrameClient;
    }

    public Result<TCPMessageConnection, TCPClientConnectionBroker.ConnectFailure<SocketAddress>> connect(
        String host,
        int port
    ) {
        var result = tcpFrameClient.connect(host, port);

        if (result.isOk()) {
            // Promote the connection to a TCPMessageConnection type.
            return Result.ok(new TCPMessageConnection(schema, streamCodecs, result.unwrap()));
        }

        return Result.err(result.unwrapErr());
    }
}
