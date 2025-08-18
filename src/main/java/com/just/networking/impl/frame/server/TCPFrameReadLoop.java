package com.just.networking.impl.frame.server;

import java.io.IOException;

import com.just.networking.impl.frame.TCPFrameConnection;
import com.just.networking.server.ReadLoop;

public final class TCPFrameReadLoop<C extends TCPFrameConnection> implements ReadLoop<C, FrameReadLoopHandler<C>> {

    @Override
    public void run(C connection, FrameReadLoopHandler<C> handler) throws IOException {
        handler.onConnect(connection);

        try {
            while (connection.isOpen()) {
                var framePayload = connection.transport().readFrame();

                if (framePayload != null) {
                    handler.onReceiveFrame(connection, framePayload);
                } else {
                    // TODO: Make a proper log.
                    System.out.println("RECEIVED NULL FRAME, THIS SHOULDN'T HAPPEN!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            handler.onDisconnect(connection);
        }
    }
}
