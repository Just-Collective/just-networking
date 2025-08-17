package com.just.networking.impl.msg;

import com.bvanseg.just.serialization.codec.stream.StreamCodec;

public interface Message<T extends Message<T>> {

    short id();

    StreamCodec<T> codec();
}
