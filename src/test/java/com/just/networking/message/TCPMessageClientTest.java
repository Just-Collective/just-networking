package com.just.networking.message;

import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.schema.StreamCodecSchema;
import com.just.codec.stream.schema.impl.ByteBufferStreamCodecSchema;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.just.networking.SafeAutoCloseable;
import com.just.networking.config.Config;
import com.just.networking.impl.message.Message;
import com.just.networking.impl.message.MessageAccess;
import com.just.networking.impl.message.client.TCPMessageClient;
import com.just.networking.impl.message.server.TCPMessageReadLoop;
import com.just.networking.impl.message.server.TCPMessageServer;
import com.just.networking.tcp.TCPTestConstants;

public class TCPMessageClientTest {

    static class MessageRegistry {

        private final AtomicInteger idCounter = new AtomicInteger(0);

        private final Map<String, Short> messageStringIdToNetworkIdMap = new HashMap<>();

        private final Map<Short, StreamCodec<? extends Message>> messageNetworkIdToStreamCodecMap = new HashMap<>();

        public void register(String stringId, StreamCodec<? extends Message> streamCodec) {
            var nextNetworkId = (short) idCounter.getAndIncrement();
            messageStringIdToNetworkIdMap.put(stringId, nextNetworkId);
            messageNetworkIdToStreamCodecMap.put(nextNetworkId, streamCodec);
        }

        public Short getNetworkId(String stringId) {
            return messageStringIdToNetworkIdMap.get(stringId);
        }

        public StreamCodec<? extends Message> getStreamCodec(short id) {
            return messageNetworkIdToStreamCodecMap.get(id);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        var messageRegistry = new MessageRegistry();
        messageRegistry.register(ChatMessage.ID, ChatMessage.STREAM_CODEC);
        messageRegistry.register(PingMessage.ID, PingMessage.STREAM_CODEC);
        messageRegistry.register(EchoMessage.ID, EchoMessage.STREAM_CODEC);

        var messageAccess = new MessageAccess() {

            // Bootstrap message "registry" and means of encode/decode.
            private final StreamCodecSchema<ByteBuffer> schema = new ByteBufferStreamCodecSchema();

            @Override
            public Short getNetworkIdOrNull(Message message) {
                return messageRegistry.getNetworkId(message.getId());
            }

            @Override
            public StreamCodec<? extends Message> getCodecOrNull(short networkId) {
                return messageRegistry.getStreamCodec(networkId);
            }

            @Override
            public StreamCodecSchema<ByteBuffer> getSchema() {
                return schema;
            }
        };

        // Bootstrap server components.
        var config = Config.DEFAULT;
        var tcpMessageServer = new TCPMessageServer(config, messageAccess);
        var serverBindResult = tcpMessageServer.bind(TCPTestConstants.HOST, TCPTestConstants.PORT);
        // Start the server.
        serverBindResult.ifOk(
            tcpMessageServerConnection -> tcpMessageServerConnection.listen(
                new TCPMessageReadLoop<>(messageAccess),
                MessageReadLoopHandlerImpl::new
            )
        );

        // Bootstrap client.
        var msgClient = new TCPMessageClient(config, messageAccess);
        // Connect the client to the server.
        var connectionResult = msgClient.connect(TCPTestConstants.HOST, TCPTestConstants.PORT);

        // Use client.
        connectionResult.ifOk(connection -> {
            MessageClientBootstrapper.startClientReader(connection);

            try {
                MessageClientBootstrapper.runClientMessageInputLoop(connection);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Sleep for 1 second to give the server a moment to ingest all the messages.
        Thread.sleep(1000);

        // Close the client first before closing the server.
        connectionResult.ifOk(SafeAutoCloseable::close);
        serverBindResult.ifOk(SafeAutoCloseable::close);
    }

}
