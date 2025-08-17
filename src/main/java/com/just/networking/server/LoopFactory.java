package com.just.networking.server;

import com.just.networking.Connection;

@FunctionalInterface
public interface LoopFactory<C extends Connection, H> {

    ReadLoop<C, H> create();
}
