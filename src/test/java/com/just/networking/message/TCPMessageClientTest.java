package com.just.networking.message;

import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.impl.ByteBufferStreamCodecSchema;

import java.io.IOException;
import java.util.Map;

import com.just.networking.SafeAutoCloseable;
import com.just.networking.config.message.TCPMessageConfig;
import com.just.networking.impl.message.Message;
import com.just.networking.impl.message.client.TCPMessageClient;
import com.just.networking.impl.message.server.TCPMessageReadLoop;
import com.just.networking.impl.message.server.TCPMessageServer;
import com.just.networking.tcp.TCPTestConstants;

public class TCPMessageClientTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        // Bootstrap message "registry" and means of encode/decode.
        var schema = new ByteBufferStreamCodecSchema();
        Map<Short, StreamCodec<? extends Message<?>>> map = Map.ofEntries(
            Map.entry((short) 1, ChatMessage.STREAM_CODEC),
            Map.entry((short) 2, PingMessage.STREAM_CODEC),
            Map.entry((short) 3, EchoMessage.STREAM_CODEC)
        );

        // Bootstrap server components.
        var tcpServerConfig = TCPMessageConfig.DEFAULT;
        var tcpMessageServer = new TCPMessageServer(tcpServerConfig, schema, map);
        var serverBindResult = tcpMessageServer.bind(TCPTestConstants.HOST, TCPTestConstants.PORT);
        // Start the server.
        serverBindResult.ifOk(
            tcpMessageServerConnection -> tcpMessageServerConnection.listen(
                new TCPMessageReadLoop<>(schema, map),
                MessageReadLoopHandlerImpl::new
            )
        );

        // Bootstrap client.
        var msgClient = new TCPMessageClient(schema, map);
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
