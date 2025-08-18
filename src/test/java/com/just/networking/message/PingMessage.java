package com.just.networking.message;

import com.bvanseg.just.serialization.codec.stream.RecordStreamCodec;
import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.impl.StreamCodecs;

import com.just.networking.impl.msg.Message;

public record PingMessage(long epochMillis) implements Message<PingMessage> {

    public static final StreamCodec<PingMessage> STREAM_CODEC = RecordStreamCodec.of(
        StreamCodecs.LONG,
        PingMessage::epochMillis,
        PingMessage::new
    );

    @Override
    public short id() {
        return 2;
    }

    @Override
    public StreamCodec<PingMessage> codec() {
        return STREAM_CODEC;
    }
}
