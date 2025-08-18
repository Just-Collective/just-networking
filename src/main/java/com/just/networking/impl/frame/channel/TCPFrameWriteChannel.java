package com.just.networking.impl.frame.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Function;

public class TCPFrameWriteChannel implements AutoCloseable {

    private final Function<ByteBuffer, Integer> writer;

    // A big direct staging buffer we pack many frames into before a write().
    // Tune size; 4–16 MiB works well. Must be >= largest single frame + 4.
    private final ByteBuffer staging;

    public TCPFrameWriteChannel(Function<ByteBuffer, Integer> writer) {
        this.writer = writer;
        this.staging = ByteBuffer.allocateDirect(4 * 1024 * 1024).order(ByteOrder.BIG_ENDIAN);
    }

    @Override
    public void close() {
        staging.clear();
    }

    public void sendFrame(ByteBuffer payload) throws IOException {
        // Work on a duplicate so we don't change caller's position/limit.
        var src = payload.duplicate();
        var len = src.remaining();
        var needed = TCPFrameChannelConstants.LEN_BYTES + len;

        if (needed <= staging.capacity()) {
            // Normal case: write the entire frame atomically into staging.
            ensureFreeSpace(needed);
            staging.putInt(len);
            staging.put(src); // copies all bytes, does NOT touch 'payload'
        } else {
            // Rare: frame is larger than staging. Stream it across multiple flushes.
            // Write header first (may flush in between).
            putIntWithAutoFlush(len);

            // Then copy payload in chunks.
            while (src.hasRemaining()) {
                if (staging.remaining() == 0) {
                    flushBlocking();
                }

                var toCopy = Math.min(staging.remaining(), src.remaining());
                var oldLimit = src.limit();
                src.limit(src.position() + toCopy);
                staging.put(src);
                src.limit(oldLimit);
            }
        }
    }

    /** Flushes whatever is in staging to the socket (blocking until fully written). */
    public void flushWrites() throws IOException {
        flushBlocking();
    }

    private void ensureFreeSpace(int needed) throws IOException {
        // If the whole frame fits into staging, flush until we have room for it.
        // If 'needed' is larger than capacity, the caller will stream it piecewise.
        if (needed > staging.capacity()) {
            return;
        }

        while (staging.remaining() < needed) {
            flushBlocking();
        }
    }

    private void putIntWithAutoFlush(int v) throws IOException {
        // We may need up to 4 bytes; write piecemeal if staging is nearly full.
        for (var shift = 24; shift >= 0; shift -= 8) {
            if (staging.remaining() == 0) {
                flushBlocking();
            }

            staging.put((byte) ((v >>> shift) & 0xFF));
        }
    }

    private void flushBlocking() throws IOException {
        if (staging.position() == 0) {
            // nothing to send.
            return;
        }

        staging.flip();

        while (staging.hasRemaining()) {
            var n = writer.apply(staging);

            if (n < 0) {
                throw new java.nio.channels.ClosedChannelException();
            }

            if (n == 0) {
                // No bytes were written out, so wait a little bit before making another attempt.
                Thread.onSpinWait();
            }
        }

        staging.clear();
    }
}
