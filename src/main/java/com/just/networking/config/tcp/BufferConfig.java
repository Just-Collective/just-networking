package com.just.networking.config.tcp;

import java.nio.ByteOrder;

public record BufferConfig(
    boolean direct,
    int ioBufferBytes,
    ByteOrder byteOrder
) {

    public static final BufferConfig DEFAULT = new BufferConfig(true, 1, ByteOrder.BIG_ENDIAN);

    public BufferConfig withDirect(boolean direct) {
        return new BufferConfig(direct, ioBufferBytes, byteOrder);
    }

    public BufferConfig withIOBufferBytes(int ioBufferBytes) {
        return new BufferConfig(direct, ioBufferBytes, byteOrder);
    }

    public BufferConfig withByteOrder(ByteOrder byteOrder) {
        return new BufferConfig(direct, ioBufferBytes, byteOrder);
    }
}
