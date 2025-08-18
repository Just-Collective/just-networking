package com.just.networking.impl.message.client;

import com.bvanseg.just.functional.result.Result;
import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.just.networking.impl.frame.client.TCPFrameClient;
import com.just.networking.impl.message.Message;
import com.just.networking.impl.message.TCPMessageConnection;

public class TCPMessageClient implements AutoCloseable {

    private volatile TCPMessageConnection tcpMessageConnection;

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

    @Override
    public void close() {
        disconnect();
    }

    public Result<TCPMessageConnection, Void> connect(String host, int port) {
        var result = tcpFrameClient.connect(host, port);

        if (result.isOk()) {
            // Promote the connection to a TCPMessageConnection type.
            this.tcpMessageConnection = new TCPMessageConnection(schema, streamCodecs, result.unwrap());
            return Result.ok(tcpMessageConnection);
        }

        return Result.err(result.unwrapErr());
    }

    public void sendMessage(Message<?> message) {
        // TODO: Null check.
        tcpMessageConnection.transport().sendMessage(message);
    }

    public Message<?> pollMessage() {
        return tcpMessageConnection.transport().pollMessage();
    }

    public void disconnect() {
        tcpFrameClient.disconnect();
    }

    public void flushWrites() throws IOException {
        tcpMessageConnection.transport().flushWrites();
    }

    public boolean isOpen() {
        return tcpFrameClient.isOpen();
    }
}
