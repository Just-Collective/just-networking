package com.just.networking.message;

import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;

import com.just.networking.impl.message.Message;

public record PingMessage(long epochMillis) implements Message {

    public static final String ID = "ping";

    public static final StreamCodec<PingMessage> STREAM_CODEC = RecordStreamCodec.of(
        StreamCodecs.LONG,
        PingMessage::epochMillis,
        PingMessage::new
    );

    @Override
    public String getId() {
        return ID;
    }
}
