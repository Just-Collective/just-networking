package com.just.networking.message;

import com.bvanseg.just.serialization.codec.stream.RecordStreamCodec;
import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.impl.StreamCodecs;

import com.just.networking.impl.msg.Message;

public record EchoMessage(String message) implements Message<EchoMessage> {

    public static final StreamCodec<EchoMessage> STREAM_CODEC = RecordStreamCodec.of(
        StreamCodecs.STRING_UTF8,
        EchoMessage::message,
        EchoMessage::new
    );

    @Override
    public short id() {
        return 3;
    }

    @Override
    public StreamCodec<EchoMessage> codec() {
        return STREAM_CODEC;
    }
}
