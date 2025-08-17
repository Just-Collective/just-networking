package com.just.networking;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface ChannelIO {

    int read(ByteBuffer dst) throws IOException;

    int write(ByteBuffer src) throws IOException;
}
