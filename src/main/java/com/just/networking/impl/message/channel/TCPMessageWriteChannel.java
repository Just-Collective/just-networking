package com.just.networking.impl.message.channel;

import com.just.codec.stream.StreamCodec;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import com.just.networking.config.Config;
import com.just.networking.config.DefaultConfigKeys;
import com.just.networking.impl.message.Message;
import com.just.networking.impl.message.MessageAccess;

public class TCPMessageWriteChannel {

    private final Config config;

    private final MessageAccess messageAccess;

    // Underlying I/O.
    private final Consumer<ByteBuffer> frameWriter;

    // Reusable scratch for encoding one message: [typeId + payload]
    private final ByteBuffer scratch;

    // Outbound message-batching buffer; flushed as a single TCP frame
    private final ByteBuffer outFrame;

    public TCPMessageWriteChannel(
        Config config,
        MessageAccess messageAccess,
        Consumer<ByteBuffer> frameWriter
    ) {
        this.config = config;
        this.messageAccess = messageAccess;
        this.frameWriter = frameWriter;

        this.scratch = ByteBuffer.allocateDirect(
            config.get(DefaultConfigKeys.TCP_MESSAGE_WRITE_CHANNEL_SCRATCH_BUFFER_SIZE_IN_BYTES)
        )
            .order(config.get(DefaultConfigKeys.TCP_MESSAGE_WRITE_CHANNEL_BYTE_ORDER));
        this.outFrame = ByteBuffer.allocateDirect(
            config.get(DefaultConfigKeys.TCP_MESSAGE_WRITE_CHANNEL_OUT_BUFFER_SIZE_IN_BYTES)
        )
            .order(config.get(DefaultConfigKeys.TCP_MESSAGE_WRITE_CHANNEL_BYTE_ORDER));
    }

    public void sendMessage(Message message) {
        final Short networkId = messageAccess.getNetworkIdOrNull(message);

        if (networkId == null) {
            throw new IllegalArgumentException("Unknown message network ID derived from message. Message: " + message);
        }

        @SuppressWarnings("unchecked")
        final StreamCodec<Message> codec = (StreamCodec<Message>) messageAccess.getCodecOrNull(networkId);

        if (codec == null) {
            throw new IllegalArgumentException("No codec found for message. Network ID: " + networkId);
        }

        // Encode into scratch: [typeId | payload]
        scratch.clear();
        scratch.putShort(networkId);
        codec.encode(messageAccess.getSchema(), scratch, message);
        scratch.flip();
        // includes 2 bytes of typeId
        final int msgBytes = scratch.remaining();

        // If the record doesn't fit in an empty outFrame, send it alone as its own frame.
        if (msgBytes > outFrame.capacity()) {
            // flush current batched messages first.
            flush();
            // Direct one-off frame for this large message
            final ByteBuffer tmp = ByteBuffer.allocateDirect(msgBytes)
                .order(config.get(DefaultConfigKeys.TCP_MESSAGE_WRITE_CHANNEL_BYTE_ORDER));
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
}
