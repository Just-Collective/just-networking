package com.just.networking;

import java.io.IOException;

public interface Transport {

    boolean isOpen();

    void close() throws IOException;
}
