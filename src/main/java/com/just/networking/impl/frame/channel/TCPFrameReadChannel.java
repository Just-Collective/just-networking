package com.just.networking.impl.frame.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.function.Function;

import com.just.networking.config.Config;
import com.just.networking.config.DefaultConfigKeys;

public class TCPFrameReadChannel implements AutoCloseable {

    private final Config config;

    private final Function<ByteBuffer, Integer> reader;

    // If current frame is larger than receiverBuffer.capacity(), we stream it into this temp.
    // null when not in use
    private ByteBuffer oversized;

    // One reusable direct buffer that we read into and peel frames from.
    private final ByteBuffer receiverBuffer;

    // -1 => need to read length next
    private int pendingLen = -1;

    public TCPFrameReadChannel(Config config, Function<ByteBuffer, Integer> reader) {
        this.config = config;
        this.reader = reader;
        this.receiverBuffer = ByteBuffer.allocateDirect(
            config.get(DefaultConfigKeys.TCP_FRAME_READ_CHANNEL_BUFFER_SIZE_IN_BYTES)
        )
            .order(config.get(DefaultConfigKeys.TCP_FRAME_READ_CHANNEL_BYTE_ORDER));

        // Keep receiverBuffer in "read mode" with no data initially.
        receiverBuffer.limit(0);
    }

    @Override
    public void close() {
        receiverBuffer.clear();
        receiverBuffer.limit(0);
        this.oversized = null;
        this.pendingLen = -1;
    }

    public ByteBuffer readFrame() throws IOException {
        // Slow path for huge frames: stream directly into a temporary buffer.
        if (oversized != null) {
            var r = reader.apply(oversized);

            if (r == -1) {
                this.oversized = null;
                this.pendingLen = -1;
                throw new ClosedChannelException();
            }

            if (oversized.hasRemaining()) {
                return null;
            }

            oversized.flip();
            var out = oversized;
            this.oversized = null;
            this.pendingLen = -1;
            // owned, standalone buffer
            return out;
        }

        // Ensure we have the length.
        if (pendingLen < 0) {
            if (receiverBuffer.remaining() < TCPFrameChannelConstants.LEN_BYTES) {
                // read more bytes
                refill();

                if (receiverBuffer.remaining() < TCPFrameChannelConstants.LEN_BYTES) {
                    return null;
                }
            }

            var len = receiverBuffer.getInt();

            if (len < 0) {
                throw new IllegalStateException("Negative frame length: " + len);
            }

            pendingLen = len;

            if (len > receiverBuffer.capacity()) {
                // Allocate a one-off buffer for this large frame.
                this.oversized = ByteBuffer.allocate(len)
                    .order(config.get(DefaultConfigKeys.TCP_FRAME_READ_CHANNEL_BYTE_ORDER));
                // Copy any payload bytes already available in receiverBuffer into oversized.
                var available = Math.min(receiverBuffer.remaining(), len);

                if (available > 0) {
                    var oldLimit = receiverBuffer.limit();
                    receiverBuffer.limit(receiverBuffer.position() + available);
                    oversized.put(receiverBuffer); // copies available bytes
                    receiverBuffer.limit(oldLimit);
                }

                // We'll continue filling 'oversized' on subsequent calls.
                return readFrame();
            }
        }

        // Ensure we have the full payload for the pending frame.
        if (receiverBuffer.remaining() < pendingLen) {
            refill();

            if (receiverBuffer.remaining() < pendingLen) {
                return null;
            }
        }

        // Produce a bounded view of the payload with no allocation/copy.
        var frameEnd = receiverBuffer.position() + pendingLen;
        var oldLimit = receiverBuffer.limit();
        receiverBuffer.limit(frameEnd);
        var view = receiverBuffer.slice()
            .order(config.get(DefaultConfigKeys.TCP_FRAME_READ_CHANNEL_BYTE_ORDER));
        receiverBuffer.limit(oldLimit);
        receiverBuffer.position(frameEnd);
        this.pendingLen = -1;

        return view;
    }

    private void refill() throws IOException {
        // Move unread bytes to the front and read more from the channel.
        receiverBuffer.compact();

        var n = reader.apply(receiverBuffer);

        if (n == -1) {
            throw new ClosedChannelException();
        }

        // ready for reading again
        receiverBuffer.flip();
    }
}
