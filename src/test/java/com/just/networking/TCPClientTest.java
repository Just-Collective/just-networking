package com.just.networking;

import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.schema.impl.ByteBufferStreamCodecSchema;

import java.io.IOException;
import java.util.Map;

import com.just.networking.impl.msg.Message;
import com.just.networking.impl.msg.client.TCPMessageClient;
import com.just.networking.impl.msg.server.TCPMessageServer;
import com.just.networking.message.ChatMessage;
import com.just.networking.message.EchoMessage;
import com.just.networking.message.PingMessage;

public class TCPClientTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        var host = "localhost";
        var port = 3000;

        // Bootstrap message "registry" and means of encode/decode.
        var schema = new ByteBufferStreamCodecSchema();
        Map<Short, StreamCodec<? extends Message<?>>> map = Map.ofEntries(
            Map.entry((short) 1, ChatMessage.STREAM_CODEC),
            Map.entry((short) 2, PingMessage.STREAM_CODEC),
            Map.entry((short) 3, EchoMessage.STREAM_CODEC)
        );

        // Bootstrap server components.
        var tcpMessageServer = new TCPMessageServer(schema, map);
        // Start the server.
        tcpMessageServer.start(host, port, MessageReadLoopHandlerImpl::new);

        // Bootstrap client.
        var msgClient = new TCPMessageClient(schema, map);
        // Connect the client to the server.
        var connectionResult = msgClient.connect(host, port);

        // Use client.
        connectionResult.ifOk(connection -> {
            ClientBootstrapper.startClientReader(msgClient);

            try {
                ClientBootstrapper.runClientMessageInputLoop(msgClient);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Sleep for 1 second to give the server a moment to ingest all the messages.
        Thread.sleep(1000);
        // Close the client first before closing the server.
        msgClient.close();
        tcpMessageServer.close();
    }

}
