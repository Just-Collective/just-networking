package com.just.networking.impl.tcp.server;

import java.nio.ByteBuffer;

import com.just.networking.config.Config;
import com.just.networking.config.DefaultConfigKeys;
import com.just.networking.impl.tcp.TCPConnection;
import com.just.networking.server.ReadLoop;

public final class TCPByteReadLoop<C extends TCPConnection> implements ReadLoop<C, ByteReadLoopHandler<C>> {

    private final Config config;

    public TCPByteReadLoop(Config config) {
        this.config = config;
    }

    @Override
    public void run(C connection, ByteReadLoopHandler<C> handler) {
        handler.onConnect(connection);

        var bufferSize = config.get(DefaultConfigKeys.TCP_READ_LOOP_BUFFER_SIZE_IN_BYTES);
        var byteOrder = config.get(DefaultConfigKeys.TCP_READ_LOOP_BYTE_ORDER);
        var byteBuffer = ByteBuffer.allocateDirect(bufferSize).order(byteOrder);

        try {
            while (connection.isOpen()) {
                var n = connection.transport().read(byteBuffer);

                if (n == -1) {
                    break;
                }

                if (n == 0) {
                    continue;
                }

                byteBuffer.flip();

                // Hand a read-only view of the bytes we just received.
                handler.onReceiveBytes(connection, byteBuffer.asReadOnlyBuffer());

                // ready for next read.
                byteBuffer.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            handler.onDisconnect(connection);
        }
    }
}
