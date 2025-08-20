package com.just.networking;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Writer {

    int write(ByteBuffer src) throws IOException;

    long write(ByteBuffer[] srcs) throws IOException;
}
