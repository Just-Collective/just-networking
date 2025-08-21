package com.just.networking.server;

import com.bvanseg.just.functional.result.Result;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.just.networking.Connection;
import com.just.networking.SafeAutoCloseable;

public interface ServerConnection<C extends Connection<?>> extends SafeAutoCloseable {

    boolean isOpen();

    Result<C, IOException> accept();

    default <H> Thread listen(ReadLoop<C, H> readLoop, Supplier<? extends H> handlerSupplier) {
        return listen("client-accept", Executors.newVirtualThreadPerTaskExecutor(), readLoop, handlerSupplier);
    }

    default <H> Thread listen(
        String acceptThreadName,
        ExecutorService clientExecutorService,
        ReadLoop<C, H> readLoop,
        Supplier<? extends H> handlerSupplier
    ) {
        return Thread.ofVirtual().name(acceptThreadName).start(() -> {
            while (isOpen()) {
                var result = accept();

                // TODO: Handle error here. What if the result fails?
                result.ifOk(
                    connection -> clientExecutorService.submit(() -> readLoop.run(connection, handlerSupplier.get()))
                );
            }
        });
    }
}
