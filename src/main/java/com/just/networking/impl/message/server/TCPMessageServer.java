package com.just.networking.impl.message.server;

import com.bvanseg.just.functional.result.Result;
import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

import com.just.networking.config.Config;
import com.just.networking.impl.frame.server.TCPFrameServer;
import com.just.networking.impl.message.Message;
import com.just.networking.impl.tcp.server.TCPServer;

public class TCPMessageServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCPMessageServer.class);

    private final Config config;

    private final StreamCodecSchema<ByteBuffer> schema;

    private final Map<Short, StreamCodec<? extends Message<?>>> streamCodecs;

    private final TCPFrameServer tcpFrameServer;

    public TCPMessageServer(
        Config config,
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs
    ) {
        this(config, schema, streamCodecs, new TCPFrameServer(config));
    }

    public TCPMessageServer(
        Config config,
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs,
        TCPFrameServer tcpFrameServer
    ) {
        this.config = config;
        this.schema = schema;
        this.streamCodecs = streamCodecs;
        this.tcpFrameServer = tcpFrameServer;
    }

    public Result<TCPMessageServerConnection, TCPServer.BindFailure<SocketAddress>> bind(
        String host,
        int port
    ) {
        return bind(new InetSocketAddress(host, port));
    }

    public Result<TCPMessageServerConnection, TCPServer.BindFailure<SocketAddress>> bind(
        SocketAddress bindAddress
    ) {
        var result = tcpFrameServer.bind(bindAddress);

        // Announce the upgrade from raw TCP to message transport.
        result.ifOk(
            $ -> LOGGER.info(
                "Upgraded TCP Frame server at {} to message transport (TCPMessageServerConnection).",
                bindAddress
            )
        );

        return result.map(
            connection -> new TCPMessageServerConnection(config, schema, streamCodecs, connection)
        );
    }
}
