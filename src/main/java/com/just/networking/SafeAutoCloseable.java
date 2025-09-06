package com.just.networking;

import com.just.core.functional.result.Result;

import java.io.IOException;

public interface SafeAutoCloseable extends AutoCloseable {

    Result<Void, IOException> closeWithResult();

    @Override
    default void close() {
        closeWithResult();
    }
}
