package com.just.networking;

import java.io.IOException;

import com.just.networking.impl.message.Message;
import com.just.networking.impl.message.TCPMessageConnection;
import com.just.networking.impl.message.server.MessageReadLoopHandler;
import com.just.networking.message.ChatMessage;
import com.just.networking.message.EchoMessage;
import com.just.networking.message.PingMessage;

public class MessageReadLoopHandlerImpl implements MessageReadLoopHandler<TCPMessageConnection> {

    private boolean authorized;

    private int x = 0;

    @Override
    public void onConnect(TCPMessageConnection connection) {
        System.out.println("Client connected.");

        if (!authorized) {
            connection.transport().sendMessage(new ChatMessage("Welcome. Please enter your password to proceed."));
            try {
                connection.transport().flushWrites();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onReceiveMessage(TCPMessageConnection connection, Message<?> m) {
        if (!authorized) {
            if (m instanceof ChatMessage(String message) && message.equals("1234")) {
                this.authorized = true;
                connection.transport().sendMessage(new ChatMessage("Authorization granted. Welcome home!"));
                try {
                    connection.transport().flushWrites();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            }

            connection.transport().sendMessage(new ChatMessage("UNAUTHORIZED. Please enter your password to proceed."));
            try {
                connection.transport().flushWrites();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        switch (m) {
            case ChatMessage(String message) -> {
                connection.transport().sendMessage(m);

                try {
                    connection.transport().flushWrites();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
            case EchoMessage(String message) -> {
                connection.transport().sendMessage(m);

                try {
                    connection.transport().flushWrites();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case PingMessage(long epochMillis) -> {
                connection.transport().sendMessage(new PingMessage(epochMillis));

                try {
                    connection.transport().flushWrites();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> {}
        }

        x++;
    }

    @Override
    public void onDisconnect(TCPMessageConnection connection) {
        System.out.println("Client disconnected. Total messages read: " + x);
    }
}
