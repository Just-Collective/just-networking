package com.just.networking;

import com.bvanseg.just.functional.result.Result;

import java.io.IOException;

public interface SafeAutoCloseable extends AutoCloseable {

    Result<Void, IOException> closeWithResult();

    @Override
    default void close() {
        closeWithResult();
    }
}
