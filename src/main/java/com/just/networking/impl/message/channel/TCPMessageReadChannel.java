package com.just.networking.impl.message.channel;

import com.just.codec.stream.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import com.just.networking.config.Config;
import com.just.networking.impl.message.Message;
import com.just.networking.impl.message.MessageAccess;

public class TCPMessageReadChannel {

    private final Supplier<ByteBuffer> frameReader;

    private final MessageAccess messageAccess;

    // Inbound state: current frame we’re draining (may contain multiple messages)
    private ByteBuffer inFrame;

    public TCPMessageReadChannel(
        Config config,
        MessageAccess messageAccess,
        Supplier<ByteBuffer> frameReader
    ) {
        this.messageAccess = messageAccess;
        this.frameReader = frameReader;
        this.inFrame = null;
    }

    public @Nullable Message pollMessage() {
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
        var codec = (StreamCodec<Message>) messageAccess.getCodecOrNull(typeId);

        if (codec == null) {
            // Unknown type, bail out and drop the rest of this frame
            this.inFrame = null;
            return null;
        }

        try {
            // Codec must consume exactly its payload from inFrame.
            // Return one message per call.
            return codec.decode(messageAccess.getSchema(), inFrame);
        } catch (Exception e) {
            // If a codec fails, drop this frame to avoid desync.
            this.inFrame = null;
            return null;
        }
    }
}
