package com.just.networking.impl.message.channel;

import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.StreamCodecSchema;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.just.networking.config.message.TCPMessageConfig;
import com.just.networking.impl.message.Message;

public class TCPMessageChannel implements AutoCloseable {

    private static final int INITIAL_CAP = 8 * 1024;

    private static final int OUT_FRAME_CAP = 64 * 1024;

    private static final ByteOrder ORDER = ByteOrder.BIG_ENDIAN;

    private final StreamCodecSchema<ByteBuffer> schema;

    private final Map<Short, StreamCodec<? extends Message<?>>> streamCodecs;

    // Underlying I/O.
    private final Consumer<ByteBuffer> frameWriter;

    private final Supplier<ByteBuffer> frameReader;

    // Reusable scratch for encoding one message: [typeId + payload]
    private final ByteBuffer scratch;

    // Outbound message-batching buffer; flushed as a single TCP frame
    private final ByteBuffer outFrame;

    // Inbound state: current frame we’re draining (may contain multiple messages)
    private ByteBuffer inFrame;

    public TCPMessageChannel(
        TCPMessageConfig tcpMessageConfig,
        StreamCodecSchema<ByteBuffer> schema,
        Map<Short, StreamCodec<? extends Message<?>>> streamCodecs,
        Supplier<ByteBuffer> frameReader,
        Consumer<ByteBuffer> frameWriter
    ) {
        this.schema = schema;
        this.streamCodecs = streamCodecs;
        this.frameReader = frameReader;
        this.frameWriter = frameWriter;

        this.scratch = ByteBuffer.allocateDirect(INITIAL_CAP).order(ORDER);
        this.outFrame = ByteBuffer.allocateDirect(OUT_FRAME_CAP).order(ORDER);
        this.inFrame = null;
    }

    @Override
    public void close() {}

    public void sendMessage(Message<?> message) {
        final short typeId = message.id();

        @SuppressWarnings("unchecked")
        final StreamCodec<Message<?>> codec = (StreamCodec<Message<?>>) streamCodecs.get(typeId);
        if (codec == null)
            throw new IllegalArgumentException("Unknown message type: " + typeId);

        // Encode into scratch: [typeId | payload]
        scratch.clear();
        scratch.putShort(typeId);
        codec.encode(schema, scratch, message);
        scratch.flip();
        // includes 2 bytes of typeId
        final int msgBytes = scratch.remaining();

        // If the record doesn't fit in an empty outFrame, send it alone as its own frame.
        if (msgBytes > outFrame.capacity()) {
            // flush current batched messages first.
            flush();
            // Direct one-off frame for this large message
            final ByteBuffer tmp = ByteBuffer.allocateDirect(msgBytes).order(ORDER);
            tmp.put(scratch.duplicate()).flip();
            frameWriter.accept(tmp);
            return;
        }

        // If it doesn't fit in the current outFrame, flush and start a fresh frame.
        if (outFrame.remaining() < msgBytes) {
            flush();
        }

        // Append record: [typeId+payload]
        outFrame.put(scratch);
    }

    /** Force-send the current outbound frame if it has any messages staged. */
    public void flush() {
        if (outFrame.position() == 0) {
            return;
        }

        outFrame.flip();
        frameWriter.accept(outFrame);
        outFrame.clear();
    }

    /**
     * Pull the next decoded message if available. If the current inbound frame runs out, fetch another from
     * {@code frameReader}.
     */
    public @Nullable Message<?> pollMessage() {
        // Grab a new frame if we’re out
        if (inFrame == null || !inFrame.hasRemaining()) {
            this.inFrame = frameReader.get();

            if (inFrame == null) {
                // nothing available.
                return null;
            }
        }

        // Not enough for a typeId → wait for next frame
        if (inFrame.remaining() < Short.BYTES) {
            this.inFrame = null;
            return null;
        }

        var typeId = inFrame.getShort();

        @SuppressWarnings("unchecked")
        var codec = (StreamCodec<Message<?>>) streamCodecs.get(typeId);

        if (codec == null) {
            // Unknown type, bail out and drop the rest of this frame
            this.inFrame = null;
            return null;
        }

        try {
            // Codec must consume exactly its payload from inFrame.
            // Return one message per call.
            return codec.decode(schema, inFrame);
        } catch (Exception e) {
            // If a codec fails, drop this frame to avoid desync.
            this.inFrame = null;
            return null;
        }
    }
}
