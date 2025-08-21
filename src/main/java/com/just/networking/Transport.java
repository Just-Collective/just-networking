package com.just.networking;

public interface Transport extends SafeAutoCloseable {

    boolean isOpen();
}
