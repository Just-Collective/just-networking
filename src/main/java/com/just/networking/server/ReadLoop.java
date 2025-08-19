package com.just.networking.server;

import com.just.networking.Connection;

public interface ReadLoop<C extends Connection<?>, H> {

    void run(C connection, H handler);
}
