package com.just.networking.impl.message;

import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.schema.StreamCodecSchema;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public interface MessageAccess {

    @Nullable
    Short getNetworkIdOrNull(Message message);

    @Nullable
    StreamCodec<? extends Message> getCodecOrNull(short networkId);

    default @Nullable StreamCodec<? extends Message> getCodecOrNull(Message message) {
        var networkId = getNetworkIdOrNull(message);

        return networkId == null
            ? null
            : getCodecOrNull(networkId);
    }

    StreamCodecSchema<ByteBuffer> getSchema();
}
