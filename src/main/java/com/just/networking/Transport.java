package com.just.networking;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Transport {

    boolean isOpen();

    int read(ByteBuffer dst) throws IOException;

    int write(ByteBuffer src) throws IOException;

    void close() throws IOException;
}
