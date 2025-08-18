package com.just.networking;

import java.io.IOException;

public interface Connection<T extends Transport> extends AutoCloseable {

    T transport();

    default boolean isOpen() {
        return transport().isOpen();
    }

    @Override
    void close() throws IOException;
}
