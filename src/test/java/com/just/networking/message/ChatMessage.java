package com.just.networking.message;

import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;

import com.just.networking.impl.message.Message;

public record ChatMessage(String message) implements Message {

    public static final String ID = "chat";

    public static final StreamCodec<ChatMessage> STREAM_CODEC = RecordStreamCodec.of(
        StreamCodecs.STRING_UTF8,
        ChatMessage::message,
        ChatMessage::new
    );

    @Override
    public String getId() {
        return ID;
    }
}
