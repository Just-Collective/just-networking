package com.just.networking.impl.message.server;

import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

import com.just.networking.config.message.TCPMessageConfig;
import com.just.networking.impl.frame.server.TCPFrameServer;
import com.just.networking.impl.message.Message;

public class TCPMessageServer {

    private final TCPMessageConfig tcpMessageConfig;

    private final StreamCodecSchema<ByteBuffer> schema;

    private final Map<Short, StreamCodec<? extends Message<?>>> streamCodecs;

    private final TCPFrameServer tcpFrameServer;

    public TCPMessageServer(
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs
    ) {
        this(TCPMessageConfig.DEFAULT, schema, streamCodecs, new TCPFrameServer(TCPMessageConfig.DEFAULT.frame()));
    }

    public TCPMessageServer(
        TCPMessageConfig tcpMessageConfig,
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs
    ) {
        this(schema, streamCodecs, new TCPFrameServer(tcpMessageConfig.frame()));
    }

    public TCPMessageServer(
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs,
        TCPFrameServer tcpFrameServer
    ) {
        this(TCPMessageConfig.DEFAULT, schema, streamCodecs, tcpFrameServer);
    }

    public TCPMessageServer(
        TCPMessageConfig tcpMessageConfig,
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs,
        TCPFrameServer tcpFrameServer
    ) {
        this.tcpMessageConfig = tcpMessageConfig;
        this.schema = schema;
        this.streamCodecs = streamCodecs;
        this.tcpFrameServer = tcpFrameServer;
    }

    public TCPMessageServerConnection bind(String host, int port) throws IOException {
        return bind(new InetSocketAddress(host, port));
    }

    public TCPMessageServerConnection bind(SocketAddress bindAddress) throws IOException {
        return new TCPMessageServerConnection(tcpMessageConfig, schema, streamCodecs, tcpFrameServer.bind(bindAddress));
    }
}
