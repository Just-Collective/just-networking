package com.just.networking.impl.message.server;

import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Supplier;

import com.just.networking.impl.frame.TCPFrameConnection;
import com.just.networking.impl.message.Message;
import com.just.networking.impl.message.TCPMessageConnection;
import com.just.networking.impl.tcp.server.TCPServer;
import com.just.networking.impl.tcp.server.TCPServerBindBroker;

public class TCPMessageServer extends TCPServer<TCPMessageConnection> {

    private final StreamCodecSchema<ByteBuffer> schema;

    private final Map<Short, StreamCodec<? extends Message<?>>> streamCodecs;

    public TCPMessageServer(
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs
    ) {
        super(
            new TCPServerBindBroker(),
            c -> new TCPMessageConnection(schema, streamCodecs, new TCPFrameConnection(c))
        );
        this.schema = schema;
        this.streamCodecs = streamCodecs;
    }

    public <H extends MessageReadLoopHandler<TCPMessageConnection>> void start(
        String host,
        int port,
        Supplier<H> handlerSupplier
    ) throws IOException {
        super.start(host, port, new TCPMessageReadLoop<>(schema, streamCodecs), handlerSupplier);
    }
}
