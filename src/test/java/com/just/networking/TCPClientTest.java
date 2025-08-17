package com.just.networking;

import com.bvanseg.just.serialization.codec.stream.RecordStreamCodec;
import com.bvanseg.just.serialization.codec.stream.StreamCodec;
import com.bvanseg.just.serialization.codec.stream.impl.StreamCodecs;
import com.bvanseg.just.serialization.codec.stream.schema.impl.ByteBufferStreamCodecSchema;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Supplier;

import com.just.networking.impl.msg.Message;
import com.just.networking.impl.msg.client.MsgClient;
import com.just.networking.impl.msg.server.MessageReadLoopHandler;
import com.just.networking.impl.msg.server.TcpMessageReadLoop;
import com.just.networking.impl.tcp.TCPConnection;
import com.just.networking.impl.tcp.server.TCPServer;

public class TCPClientTest {

    public record ChatMessage(String message) implements Message<ChatMessage> {

        public static final StreamCodec<ChatMessage> STREAM_CODEC = RecordStreamCodec.of(
            StreamCodecs.STRING_UTF8,
            ChatMessage::message,
            ChatMessage::new
        );

        @Override
        public short id() {
            return 1;
        }

        @Override
        public StreamCodec<ChatMessage> codec() {
            return STREAM_CODEC;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        var host = "localhost";
        var port = 3000;
        // Bootstrap message "registry" and means of encode/decode.
        Map<Short, StreamCodec<? extends Message<?>>> map = Map.of((short) 1, ChatMessage.STREAM_CODEC);
        var schema = new ByteBufferStreamCodecSchema();

        // Bootstrap server components.
        var tcpServer = new TCPServer();
        var loop = new TcpMessageReadLoop<>(map, schema);
        // NO-OP handler for messages.
        Supplier<MessageReadLoopHandler<TCPConnection>> handlerSupplier = () -> new MessageReadLoopHandler<>() {

            @Override
            public void onConnect(TCPConnection connection) {
                System.out.println("Client connected.");
            }

            @Override
            public void onReceiveMessage(TCPConnection connection, Message<?> message) {
                System.out.println(message);
            }

            @Override
            public void onDisconnect(TCPConnection connection) {
                System.out.println("Client disconnected.");
            }
        };
        // Start the server.
        tcpServer.start(host, port, loop, handlerSupplier);

        // Bootstrap client.
        var msgClient = new MsgClient(map, schema);
        // Connect the client to the server.
        var connectionResult = msgClient.connect(host, port);

        // Use client.
        if (connectionResult instanceof ConnectionBroker.ConnectResult.Connected) {
            runClientMessageInputLoop(msgClient);
        }

        // Sleep for 1 second to give the server a moment to ingest all the messages.
        Thread.sleep(1000);
        // Close the client first before closing the server.
        msgClient.close();
        tcpServer.close();
    }

    private static void runClientMessageInputLoop(MsgClient msgClient) throws IOException {
        var scanner = new Scanner(System.in);

        while (true) {
            var line = scanner.nextLine();

            if (Objects.equals(line, "END")) {
                break;
            }

            msgClient.sendMessage(new ChatMessage(line));
            msgClient.flushWrites();
        }
    }
}
