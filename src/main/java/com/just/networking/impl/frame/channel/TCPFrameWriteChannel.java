package com.just.networking.impl.frame.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;

import com.just.networking.Writer;
import com.just.networking.config.frame.TCPFrameConfig;

public class TCPFrameWriteChannel implements AutoCloseable {

    // Reusable 4B header buffer (big-endian). One allocation total; reused per frame.
    private final ByteBuffer header;

    // A big direct staging buffer we pack many frames into before a write(). Must be >= largest single frame + 4.
    private final ByteBuffer staging;

    // Reused array for gathering writes (header and body).
    private final ByteBuffer[] vec;

    private final Writer writer;

    public TCPFrameWriteChannel(TCPFrameConfig tcpFrameConfig, Writer writer) {
        this.header = ByteBuffer.allocateDirect(TCPFrameChannelConstants.LEN_BYTES)
            .order(ByteOrder.BIG_ENDIAN);
        this.staging = ByteBuffer.allocateDirect(4 * 1024 * 1024).order(ByteOrder.BIG_ENDIAN);
        this.vec = new ByteBuffer[2];
        this.writer = writer;
    }

    @Override
    public void close() {
        header.clear();
        staging.clear();
        this.vec[0] = null;
        this.vec[1] = null;
    }

    public void sendFrame(ByteBuffer payload) throws IOException {
        final var srcPosition = payload.position();
        final var srcLimit = payload.limit();
        final var length = srcLimit - srcPosition;

        // ---- Large-frame fast path (no staging copy, single syscall) ----
        if (length >= staging.capacity()) {
            writeOutDirectly(payload, length);
            // Restore caller view
            payload.position(srcPosition);
            payload.limit(srcLimit);
            return;
        }

        // ---- Large frame path: STREAM VIA STAGING ----
        // Write the 4-byte header into staging, flushing if needed.
        writeToStaging(payload, length, srcPosition);

        // Restore caller view
        payload.limit(srcLimit);
        payload.position(srcPosition);
    }

    private void writeToStaging(ByteBuffer payload, int length, int srcPosition) throws IOException {
        writeHeaderToStaging(length);

        var currentPosition = srcPosition;
        var remaining = length;

        while (remaining > 0) {
            if (staging.remaining() == 0) {
                flushStagingBlocking();
            }

            var toCopy = staging.remaining();

            if (toCopy > remaining) {
                toCopy = remaining;
            }

            payload.limit(currentPosition + toCopy).position(currentPosition);
            staging.put(payload);

            currentPosition += toCopy;
            remaining -= toCopy;
        }
    }

    private void writeOutDirectly(ByteBuffer payload, int length) throws IOException {
        // Flush whatever we’ve batched so far.
        flushStagingBlocking();

        // Prepare reusable header (4 bytes, BE)
        header.clear();
        header.putInt(length).flip();

        // Duplicate payload so we don't mutate caller's buffer
        var body = payload.duplicate();

        // Reuse the same array each call (no per-call allocation)
        this.vec[0] = header;
        this.vec[1] = body;

        while (header.hasRemaining() || body.hasRemaining()) {
            var numberOfBytesWritten = writer.write(vec);

            if (numberOfBytesWritten < 0) {
                throw new ClosedChannelException();
            }

            if (numberOfBytesWritten == 0) {
                // tiny backoff if non-blocking
                Thread.onSpinWait();
            }
        }
    }

    /** Flushes whatever is in staging to the socket (blocking until fully written). */
    public void flushWrites() throws IOException {
        flushStagingBlocking();
    }

    private void writeHeaderToStaging(int len) throws IOException {
        // Ensure we have 4 bytes available, then write the length in one shot.
        if (staging.remaining() < TCPFrameChannelConstants.LEN_BYTES) {
            flushStagingBlocking();
        }

        staging.putInt(len);
    }

    private void flushStagingBlocking() throws IOException {
        if (staging.position() == 0) {
            // nothing to send.
            return;
        }

        staging.flip();

        while (staging.hasRemaining()) {
            var n = writer.write(staging);

            if (n < 0) {
                throw new ClosedChannelException();
            }

            if (n == 0) {
                // No bytes were written out, so wait a little bit before making another attempt.
                Thread.onSpinWait();
            }
        }

        staging.clear();
    }
}
