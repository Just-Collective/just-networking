package com.just.networking;

public interface Connection<T extends Transport> extends SafeAutoCloseable {

    T transport();

    default boolean isOpen() {
        return transport().isOpen();
    }
}
