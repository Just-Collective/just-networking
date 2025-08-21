package com.just.networking.server;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.just.networking.Connection;
import com.just.networking.SafeAutoCloseable;

public interface ServerConnection<C extends Connection<?>> extends SafeAutoCloseable {

    boolean isOpen();

    C accept() throws IOException;

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
                try {
                    var connection = accept();
                    clientExecutorService.submit(() -> readLoop.run(connection, handlerSupplier.get()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
