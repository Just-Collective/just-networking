package com.just.networking;

import com.bvanseg.just.functional.result.Result;

@FunctionalInterface
public interface ConnectionBroker<C extends Connection<?>, Target> {

    Result<C, Void> connect(Target ctx);

}
