package com.just.networking;

import java.io.IOException;

public interface Connection extends AutoCloseable {

    Transport transport();

    default boolean isOpen() {
        return transport().isOpen();
    }

    @Override
    void close() throws IOException;
}
