package com.just.networking.impl.frame.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.just.networking.impl.tcp.TCPConnection;
import com.just.networking.server.ReadLoop;

public final class TcpFrameReadLoop<C extends TCPConnection> implements ReadLoop<C, FrameReadLoopHandler<C>> {

    private final int maxFrameSize;

    private final int recvBufferSize;

    public TcpFrameReadLoop() {
        this(8 * 1024 * 1024, 256 * 1024);
    } // 8 MiB max frame, 256 KiB receive buf

    public TcpFrameReadLoop(int maxFrameSize) {
        this(maxFrameSize, 256 * 1024);
    }

    public TcpFrameReadLoop(int maxFrameSize, int recvBufferSize) {
        this.maxFrameSize = Math.max(1024, maxFrameSize);
        this.recvBufferSize = Math.max(16 * 1024, recvBufferSize);
    }

    @Override
    public void run(C connection, FrameReadLoopHandler<C> handler) throws IOException {
        handler.onConnect(connection);

        // One big direct buffer reused forever.
        ByteBuffer buf = ByteBuffer.allocateDirect(recvBufferSize).order(ByteOrder.BIG_ENDIAN);

        try {
            while (connection.isOpen()) {
                int n = connection.transport().read(buf);

                if (n == -1) {
                    break;
                }
                // non-blocking case
                if (n == 0) {
                    continue;
                }

                buf.flip();

                // Peel as many complete frames as are available.
                while (true) {
                    if (buf.remaining() < Integer.BYTES) {
                        // Not enough for length; preserve leftovers.
                        buf.compact();
                        break;
                    }

                    buf.mark();
                    int frameLen = buf.getInt();

                    if (frameLen < 0 || frameLen > maxFrameSize) {
                        throw new IOException("Invalid frame length: " + frameLen);
                    }

                    if (buf.remaining() < frameLen) {
                        // Incomplete frame: rewind and compact to preserve it for the next read.
                        buf.reset();
                        buf.compact();
                        break;
                    }

                    // Bound to this frame: [position .. position + frameLen)
                    int frameEnd = buf.position() + frameLen;
                    int oldLimit = buf.limit();
                    buf.limit(frameEnd);

                    // Hand a read-only slice to the handler (zero-copy).
                    ByteBuffer payload = buf.slice().asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
                    handler.onReceiveFrame(connection, frameLen, payload);

                    // Advance to the next frame.
                    buf.limit(oldLimit);
                    buf.position(frameEnd);

                    if (!buf.hasRemaining()) {
                        buf.clear(); // fully consumed; ready for the next read
                        break;
                    }
                }
            }
        } finally {
            handler.onDisconnect(connection);
        }
    }
}
