package com.just.networking.impl.frame.server;

import java.io.IOException;

import com.just.networking.impl.frame.TCPFrameConnection;
import com.just.networking.server.ReadLoop;

public final class TCPFrameReadLoop<C extends TCPFrameConnection> implements ReadLoop<C, FrameReadLoopHandler<C>> {

    @Override
    public void run(C connection, FrameReadLoopHandler<C> handler) {
        handler.onConnect(connection);

        try {
            while (connection.isOpen()) {
                try {
                    var framePayload = connection.transport().readFrame();

                    // TODO: Decide what to do here if the frame payload *is* null.
                    if (framePayload != null) {
                        handler.onReceiveFrame(connection, framePayload);
                    }
                } catch (IOException e) {
                    handler.onFrameReadError(connection, e);
                }
            }
        } finally {
            handler.onDisconnect(connection);
        }
    }
}
