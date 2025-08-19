package com.just.networking.config.frame;

import java.util.function.UnaryOperator;

import com.just.networking.config.tcp.TCPConfig;

public record TCPFrameConfig(
    FrameConfig frame,
    TCPConfig tcp
) {

    public static final TCPFrameConfig DEFAULT = new TCPFrameConfig(FrameConfig.DEFAULT, TCPConfig.DEFAULT);

    public TCPFrameConfig withFrameConfig(UnaryOperator<FrameConfig> unaryOperator) {
        return new TCPFrameConfig(unaryOperator.apply(frame), tcp);
    }

    public TCPFrameConfig withTCPConfig(UnaryOperator<TCPConfig> unaryOperator) {
        return new TCPFrameConfig(frame, unaryOperator.apply(tcp));
    }
}
