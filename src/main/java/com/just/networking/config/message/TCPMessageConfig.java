package com.just.networking.config.message;

import java.util.function.UnaryOperator;

import com.just.networking.config.frame.TCPFrameConfig;

public record TCPMessageConfig(
    MessageConfig message,
    TCPFrameConfig frame
) {

    public static final TCPMessageConfig DEFAULT = new TCPMessageConfig(MessageConfig.DEFAULT, TCPFrameConfig.DEFAULT);

    public TCPMessageConfig withMessageConfig(UnaryOperator<MessageConfig> unaryOperator) {
        return new TCPMessageConfig(unaryOperator.apply(message), frame);
    }

    public TCPMessageConfig withFrameConfig(UnaryOperator<TCPFrameConfig> unaryOperator) {
        return new TCPMessageConfig(message, unaryOperator.apply(frame));
    }
}
