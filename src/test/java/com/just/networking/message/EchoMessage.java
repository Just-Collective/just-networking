package com.just.networking.message;

import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;

import com.just.networking.impl.message.Message;

public record EchoMessage(String message) implements Message {

    public static final String ID = "echo";

    public static final StreamCodec<EchoMessage> STREAM_CODEC = RecordStreamCodec.of(
        StreamCodecs.STRING_UTF8,
        EchoMessage::message,
        EchoMessage::new
    );

    @Override
    public String getId() {
        return ID;
    }
}
