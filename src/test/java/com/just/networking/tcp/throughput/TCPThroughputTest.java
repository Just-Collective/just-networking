package com.just.networking.tcp.throughput;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import com.just.networking.impl.tcp.client.TCPClient;
import com.just.networking.impl.tcp.server.TCPServer;
import com.just.networking.tcp.TCPTestConstants;

public class TCPThroughputTest {

    public static void main(String[] args) throws Exception {
        var server = new TCPServer();

        var serverBindResult = server.bind(TCPTestConstants.HOST, TCPTestConstants.PORT);

        var latch = new CountDownLatch(1);
        var bytesReceived = new AtomicLong();

        serverBindResult.ifOk(tcpServerConnection -> {
            tcpServerConnection.listen(() -> (connection, data) -> {
                bytesReceived.addAndGet(data.remaining());

                if (bytesReceived.get() >= TCPTestConstants.TOTAL_BYTES) {
                    latch.countDown();
                }
            });
        });

        var client = new TCPClient();
        var connectionResult = client.connect(TCPTestConstants.HOST, TCPTestConstants.PORT);

        connectionResult.ifOk(conn -> new Thread(() -> {
            try {
                var buf = ByteBuffer.allocateDirect(TCPTestConstants.CHUNK_SIZE);
                var sent = 0L;
                var startTime = System.nanoTime();

                while (sent < TCPTestConstants.TOTAL_BYTES) {
                    buf.clear();
                    var toWrite = (int) Math.min(
                        TCPTestConstants.CHUNK_SIZE,
                        TCPTestConstants.TOTAL_BYTES - sent
                    );
                    // contents already in memory; we just bound the slice

                    while (buf.hasRemaining()) {
                        conn.transport().write(buf);
                    }

                    sent += toWrite;
                }

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

        System.out.printf("Server received %d MB%n", bytesReceived.get() / TCPTestConstants.MB_SIZE);

        serverBindResult.ifOk(tcpServerConnection -> {
            try {
                tcpServerConnection.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
