package com.just.networking;

import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

import com.just.networking.impl.message.TCPMessageConnection;
import com.just.networking.message.ChatMessage;
import com.just.networking.message.EchoMessage;
import com.just.networking.message.PingMessage;

public class ClientBootstrapper {

    public static void runClientMessageInputLoop(TCPMessageConnection connection) throws IOException {
        var scanner = new Scanner(System.in);
        var transport = connection.transport();

        while (true) {
            var line = scanner.nextLine();

            if (Objects.equals(line, "END")) {
                break;
            } else if (line.startsWith("SPAM")) {
                runClientThroughputStressTest(connection);
            } else if (line.startsWith("PING")) {
                transport.sendMessage(new PingMessage(System.currentTimeMillis()));
                transport.flushWrites();
            } else if (line.startsWith("ECHO")) {
                transport.sendMessage(new EchoMessage(line));
                transport.flushWrites();
            } else {
                transport.sendMessage(new ChatMessage(line));
                transport.flushWrites();
            }

        }
    }

    public static void runClientThroughputStressTest(TCPMessageConnection connection) throws IOException {
        var start = System.currentTimeMillis();
        var transport = connection.transport();

        for (var i = 0; i < 1_000; i++) {
            transport.sendMessage(new ChatMessage("Hello, world!"));
        }

        transport.flushWrites();

        var end = System.currentTimeMillis() - start;
        System.out.println("Took: " + end + "ms");
    }

    public static void startClientReader(TCPMessageConnection connection) {
        var transport = connection.transport();

        Thread.startVirtualThread(() -> {
            while (transport.isOpen()) {
                var pollMessage = transport.pollMessage();

                if (pollMessage != null) {
                    switch (pollMessage) {
                        case ChatMessage(String message) -> System.out.println("[SERVER] " + message);
                        case EchoMessage(String message) -> System.out.println("[SERVER] Echo: " + message);
                        case PingMessage(long epochMillis) -> {
                            var end = System.currentTimeMillis() - epochMillis;
                            System.out.println("[SERVER] Pong: " + end + "ms");
                        }
                        default -> {}
                    }
                } else {
                    // avoid busy CPU.
                    Thread.onSpinWait();
                }
            }
        });
    }
}
