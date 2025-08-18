package com.just.networking;

import com.bvanseg.just.functional.result.Result;

@FunctionalInterface
public interface ClientConnectionBroker<C extends Connection<?>, T, E> {

    Result<C, E> connect(T ctx);
}
