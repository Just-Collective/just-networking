package com.just.networking.impl.message.server;

import com.bvanseg.just.functional.result.Result;
import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

import com.just.networking.config.message.TCPMessageConfig;
import com.just.networking.impl.frame.server.TCPFrameServer;
import com.just.networking.impl.message.Message;
import com.just.networking.impl.tcp.server.TCPServer;

public class TCPMessageServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCPMessageServer.class);

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

    public Result<TCPMessageServerConnection, TCPServer.BindFailure<SocketAddress>> bind(
        String host,
        int port
    ) throws IOException {
        return bind(new InetSocketAddress(host, port));
    }

    public Result<TCPMessageServerConnection, TCPServer.BindFailure<SocketAddress>> bind(
        SocketAddress bindAddress
    ) throws IOException {
        var result = tcpFrameServer.bind(bindAddress);

        // Announce the upgrade from raw TCP to message transport.
        result.ifOk(
            $ -> LOGGER.info(
                "Upgraded TCP Frame server at {} to message transport (TCPMessageServerConnection).",
                bindAddress
            )
        );

        return result.map(
            connection -> new TCPMessageServerConnection(tcpMessageConfig, schema, streamCodecs, connection)
        );
    }
}
