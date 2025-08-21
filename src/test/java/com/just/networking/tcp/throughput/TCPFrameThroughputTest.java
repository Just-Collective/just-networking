package com.just.networking.tcp.throughput;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;

import com.just.networking.config.frame.TCPFrameConfig;
import com.just.networking.impl.frame.client.TCPFrameClient;
import com.just.networking.impl.frame.server.TCPFrameServer;
import com.just.networking.tcp.TCPTestConstants;

public class TCPFrameThroughputTest {

    public static void main(String[] args) throws Exception {
        var server = new TCPFrameServer();

        var serverBindResult = server.bind(TCPTestConstants.HOST, TCPTestConstants.PORT);

        var latch = new CountDownLatch(1);
        var bytesReceived = new LongAdder();

        serverBindResult.ifOk(tcpFrameServerConnection -> {
            tcpFrameServerConnection.listen(() -> (connection, payload) -> {
                bytesReceived.add(payload.remaining());

                if (bytesReceived.longValue() >= TCPTestConstants.TOTAL_BYTES) {
                    latch.countDown();
                }
            });
        });

        var client = new TCPFrameClient(TCPFrameConfig.DEFAULT);
        var connectionResult = client.connect(TCPTestConstants.HOST, TCPTestConstants.PORT);

        connectionResult.ifOk(conn -> new Thread(() -> {
            try {
                var buf = ByteBuffer.allocateDirect(TCPTestConstants.CHUNK_SIZE);
                var sent = 0L;
                var startTime = System.nanoTime();

                while (sent < TCPTestConstants.TOTAL_BYTES) {
                    // Bound this frame's payload size.
                    var toWrite = (int) Math.min(
                        TCPTestConstants.CHUNK_SIZE,
                        TCPTestConstants.TOTAL_BYTES - sent
                    );

                    buf.clear();
                    buf.limit(toWrite);
                    conn.transport().sendFrame(buf);

                    // Advance our logical counter; no need to advance buf.position().
                    sent += toWrite;
                }

                // Make sure to flush after the loop ends.
                conn.transport().flushWrites();

                var endTime = System.nanoTime();
                var seconds = (endTime - startTime) / 1_000_000_000.0;
                var mbps = TCPTestConstants.TOTAL_MEGABYTES / seconds;

                System.out.printf(
                    "Client finished sending %d MB in %.3f s (%.2f MB/s)%n",
                    TCPTestConstants.TOTAL_MEGABYTES,
                    seconds,
                    mbps
                );
                conn.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start());

        latch.await();

        System.out.printf("Server received %d MB%n", bytesReceived.longValue() / TCPTestConstants.MB_SIZE);

        serverBindResult.ifOk(tcpFrameServerConnection -> {
            try {
                tcpFrameServerConnection.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
