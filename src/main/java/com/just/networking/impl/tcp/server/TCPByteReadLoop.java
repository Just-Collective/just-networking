package com.just.networking.impl.tcp.server;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.just.networking.impl.tcp.TCPConnection;
import com.just.networking.server.ReadLoop;

public final class TCPByteReadLoop<C extends TCPConnection> implements ReadLoop<C, ByteReadLoopHandler<C>> {

    private final int bufferSize;

    public TCPByteReadLoop() {
        this(64 * 1024);
    }

    public TCPByteReadLoop(int bufferSize) {
        this.bufferSize = Math.max(4096, bufferSize);
    }

    @Override
    public void run(C connection, ByteReadLoopHandler<C> handler) {
        handler.onConnect(connection);

        var buf = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.BIG_ENDIAN);

        try {
            while (connection.isOpen()) {
                var n = connection.transport().read(buf);

                if (n == -1) {
                    break;
                }

                if (n == 0) {
                    continue;
                }

                buf.flip();

                // Hand a read-only view of the bytes we just received.
                handler.onReceiveBytes(connection, buf.asReadOnlyBuffer());

                // ready for next read.
                buf.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            handler.onDisconnect(connection);
        }
    }
}
