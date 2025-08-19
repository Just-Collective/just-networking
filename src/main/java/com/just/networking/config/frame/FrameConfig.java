package com.just.networking.config.frame;

import java.nio.ByteOrder;

public record FrameConfig(
    ByteOrder byteOrder,
    int maxFrameBytes,
    FrameLengthEncoding lengthEncoding
) {

    public static final FrameConfig DEFAULT = new FrameConfig(ByteOrder.BIG_ENDIAN, 1, FrameLengthEncoding.VARINT);

    public FrameConfig withByteOrder(ByteOrder byteOrder) {
        return new FrameConfig(byteOrder, maxFrameBytes, lengthEncoding);
    }

    public FrameConfig withMaxFrameBytes(int maxFrameBytes) {
        return new FrameConfig(byteOrder, maxFrameBytes, lengthEncoding);
    }

    public FrameConfig withLengthEncoding(FrameLengthEncoding lengthEncoding) {
        return new FrameConfig(byteOrder, maxFrameBytes, lengthEncoding);
    }
}
