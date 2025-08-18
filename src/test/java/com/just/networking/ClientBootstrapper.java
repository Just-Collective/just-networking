package com.just.networking;

import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

import com.just.networking.impl.msg.client.TCPMessageClient;
import com.just.networking.message.ChatMessage;
import com.just.networking.message.EchoMessage;
import com.just.networking.message.PingMessage;

public class ClientBootstrapper {

    public static void runClientMessageInputLoop(TCPMessageClient TCPMessageClient) throws IOException {
        var scanner = new Scanner(System.in);

        while (true) {
            var line = scanner.nextLine();

            if (Objects.equals(line, "END")) {
                break;
            } else if (line.startsWith("SPAM")) {
                runClientThroughputStressTest(TCPMessageClient);
            } else if (line.startsWith("PING")) {
                TCPMessageClient.sendMessage(new PingMessage(System.currentTimeMillis()));
                TCPMessageClient.flushWrites();
            } else if (line.startsWith("ECHO")) {
                TCPMessageClient.sendMessage(new EchoMessage(line));
                TCPMessageClient.flushWrites();
            } else {
                TCPMessageClient.sendMessage(new ChatMessage(line));
                TCPMessageClient.flushWrites();
            }

        }
    }

    public static void runClientThroughputStressTest(TCPMessageClient TCPMessageClient) throws IOException {
        var start = System.currentTimeMillis();

        for (var i = 0; i < 1_000; i++) {
            TCPMessageClient.sendMessage(new ChatMessage("Hello, world!"));
        }

        TCPMessageClient.flushWrites();

        var end = System.currentTimeMillis() - start;
        System.out.println("Took: " + end + "ms");
    }

    public static void startClientReader(TCPMessageClient TCPMessageClient) {
        Thread.startVirtualThread(() -> {
            while (TCPMessageClient.isOpen()) {
                var m = TCPMessageClient.pollMessage();

                if (m != null) {
                    switch (m) {
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
