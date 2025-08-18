package com.just.networking.impl.frame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;

import com.just.networking.ChannelIO;

public class TCPFrameChannel implements AutoCloseable {

    // Length prefix is a 4-byte big-endian int that counts bytes after itself.
    private static final int LEN_BYTES = Integer.BYTES;

    // Underlying I/O.
    private final ChannelIO channelIO;

    // Write state.
    // A big direct staging buffer we pack many frames into before a write().
    // Tune size; 4–16 MiB works well. Must be >= largest single frame + 4.
    private final ByteBuffer staging;

    // ----- READ STATE (optimized) -----
    // One reusable direct buffer that we read into and peel frames from.
    // Start at 256 KiB; tune as needed.
    private final ByteBuffer recv;

    // If current frame is larger than recv.capacity(), we stream it into this temp.
    // null when not in use
    private ByteBuffer oversized;

    // -1 => need to read length next
    private int pendingLen = -1;

    public TCPFrameChannel(ChannelIO channelIO) {
        this.channelIO = channelIO;
        this.staging = ByteBuffer.allocateDirect(4 * 1024 * 1024).order(ByteOrder.BIG_ENDIAN);
        this.recv = ByteBuffer.allocateDirect(256 * 1024).order(ByteOrder.BIG_ENDIAN);

        // Keep recv in "read mode" with no data initially.
        recv.limit(0);
    }

    @Override
    public void close() {
        staging.clear();
        recv.clear();
        recv.limit(0);
        oversized = null;
        pendingLen = -1;
    }

    public ByteBuffer readFrame() throws IOException {
        // Slow path for huge frames: stream directly into a temporary buffer.
        if (oversized != null) {
            int r = channelIO.read(oversized);
            if (r == -1) {
                oversized = null;
                pendingLen = -1;
                throw new ClosedChannelException();
            }
            if (oversized.hasRemaining())
                return null;
            oversized.flip();
            ByteBuffer out = oversized;
            oversized = null;
            pendingLen = -1;
            return out; // owned, standalone buffer
        }

        // Ensure we have the length.
        if (pendingLen < 0) {
            if (recv.remaining() < LEN_BYTES) {
                refill(); // read more bytes
                if (recv.remaining() < LEN_BYTES)
                    return null;
            }
            int len = recv.getInt();
            if (len < 0)
                throw new IllegalStateException("Negative frame length: " + len);
            pendingLen = len;

            if (len > recv.capacity()) {
                // Allocate a one-off buffer for this large frame.
                oversized = ByteBuffer.allocate(len).order(ByteOrder.BIG_ENDIAN);
                // Copy any payload bytes already available in recv into oversized.
                int available = Math.min(recv.remaining(), len);
                if (available > 0) {
                    int oldLimit = recv.limit();
                    recv.limit(recv.position() + available);
                    oversized.put(recv); // copies available bytes
                    recv.limit(oldLimit);
                }
                // We'll continue filling 'oversized' on subsequent calls.
                return readFrame();
            }
        }

        // Ensure we have the full payload for the pending frame.
        if (recv.remaining() < pendingLen) {
            refill();
            if (recv.remaining() < pendingLen)
                return null;
        }

        // Produce a bounded view of the payload with no allocation/copy.
        int frameEnd = recv.position() + pendingLen;
        int oldLimit = recv.limit();
        recv.limit(frameEnd);
        ByteBuffer view = recv.slice().order(ByteOrder.BIG_ENDIAN);
        recv.limit(oldLimit);
        recv.position(frameEnd);
        pendingLen = -1;
        return view;
    }

    private void refill() throws IOException {
        // Move unread bytes to the front and read more from the channel.
        recv.compact();
        int n = channelIO.read(recv);
        if (n == -1)
            throw new ClosedChannelException();
        recv.flip(); // ready for reading again
    }

    public void sendFrame(ByteBuffer payload) throws IOException {
        // Work on a duplicate so we don't change caller's position/limit.
        var src = payload.duplicate();
        var len = src.remaining();
        var needed = LEN_BYTES + len;

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
            var n = channelIO.write(staging);

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
