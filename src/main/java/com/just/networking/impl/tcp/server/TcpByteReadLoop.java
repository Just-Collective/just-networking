package com.just.networking.impl.tcp.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.just.networking.impl.tcp.TCPConnection;
import com.just.networking.server.ReadLoop;

public final class TcpByteReadLoop<C extends TCPConnection> implements ReadLoop<C, ByteReadLoopHandler<C>> {

    private final int bufferSize;

    public TcpByteReadLoop() {
        this(64 * 1024);
    }

    public TcpByteReadLoop(int bufferSize) {
        this.bufferSize = Math.max(4096, bufferSize);
    }

    @Override
    public void run(C connection, ByteReadLoopHandler<C> handler) throws IOException {
        handler.onConnect(connection);

        var buf = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.BIG_ENDIAN);

        try {
            while (connection.isOpen()) {
                int n = connection.transport().read(buf);
                if (n == -1)
                    break;
                if (n == 0)
                    continue; // for non-blocking cases

                buf.flip();

                // Hand a read-only view of the bytes we just received.
                handler.onReceiveBytes(connection, buf.asReadOnlyBuffer());

                buf.clear(); // ready for next read
            }
        } finally {
            handler.onDisconnect(connection);
        }
    }
}
