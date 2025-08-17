package com.just.networking.impl.frame.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;

import com.just.networking.ConnectionBroker;
import com.just.networking.impl.tcp.client.TCPClient;

public final class TCPFrameClient implements AutoCloseable {

    // Length prefix is a 4-byte big-endian int that counts bytes after itself.
    private static final int LEN_BYTES = Integer.BYTES;

    private final TCPClient tcpClient;

    // Read state.
    private final ByteBuffer lenBuf = ByteBuffer.allocate(LEN_BYTES).order(ByteOrder.BIG_ENDIAN);

    // Null means we are currently reading the length.
    private ByteBuffer frameBuf;

    // Write state.
    // A big direct staging buffer we pack many frames into before a write().
    // Tune size; 4–16 MiB works well. Must be >= largest single frame + 4.
    private final ByteBuffer staging =
        ByteBuffer.allocateDirect(4 * 1024 * 1024).order(ByteOrder.BIG_ENDIAN);

    public TCPFrameClient() {
        this(new TCPClient());
    }

    public TCPFrameClient(TCPClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    public ConnectionBroker.ConnectResult connect(String host, int port) {
        return tcpClient.connect(host, port);
    }

    @Override
    public void close() {
        disconnect();
    }

    public void disconnect() {
        tcpClient.disconnect();
        staging.clear();
        lenBuf.clear();
        this.frameBuf = null;
    }

    public boolean isOpen() {
        return tcpClient.isOpen();
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
            var n = tcpClient.write(staging);

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

    public ByteBuffer readFrame() throws IOException {
        // Step 1: read length prefix.
        if (frameBuf == null) {
            var n = tcpClient.read(lenBuf);

            if (n == -1) {
                throw new ClosedChannelException();
            }

            if (lenBuf.hasRemaining()) {
                // Need more bytes for the prefix.
                return null;
            }

            lenBuf.flip();
            var frameLen = lenBuf.getInt();
            lenBuf.clear();

            if (frameLen < 0) {
                throw new IllegalStateException("Negative frame length: " + frameLen);
            }

            // Allocate buffer for payload only.
            this.frameBuf = ByteBuffer.allocate(frameLen).order(ByteOrder.BIG_ENDIAN);
        }

        // Step 2: fill frame payload.
        var r = tcpClient.read(frameBuf);

        if (r == -1) {
            frameBuf = null;
            throw new ClosedChannelException();
        }

        if (frameBuf.hasRemaining()) {
            return null;
        }

        // Complete frame: flip and hand a duplicate to the caller.
        frameBuf.flip();
        var complete = frameBuf;
        this.frameBuf = null;

        return complete;
    }
}
