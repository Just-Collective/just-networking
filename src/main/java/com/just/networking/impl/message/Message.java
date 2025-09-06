package com.just.networking.impl.message;

import com.just.codec.stream.StreamCodec;

public interface Message<T extends Message<T>> {

    short id();

    StreamCodec<T> codec();
}
