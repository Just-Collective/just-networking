package com.just.networking.tcp.latency;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import com.just.networking.SafeAutoCloseable;
import com.just.networking.config.Config;
import com.just.networking.impl.tcp.client.TCPClient;
import com.just.networking.impl.tcp.server.TCPServer;
import com.just.networking.tcp.TCPTestConstants;

public class TCPLatencyTest {

    // ---- Tuning knobs ----
    static final int PAYLOAD_BYTES = 32;

    static final int WARMUP_MESSAGES = 5_000;

    static final int MEASURE_MESSAGES = 50_000;

    public static void main(String[] args) throws Exception {
        var config = Config.DEFAULT;
        var server = new TCPServer(config);
        var serverBindResult = server.bind(TCPTestConstants.HOST, TCPTestConstants.PORT);

        serverBindResult.ifOk(tcpServerConnection -> {
            // Echo server: write back whatever we read.
            tcpServerConnection.listen(() -> (connection, data) -> {
                // Assumes 'data' is already in read mode with the frame/chunk contents.
                // Echo back the same bytes (duplicate so we don't mutate the handler buffer).
                var echo = data.duplicate();
                try {
                    while (echo.hasRemaining()) {
                        connection.transport().write(echo);
                    }
                } catch (IOException e) {
                    // swallow for test; server loop will close on error
                }
            });
        });

        var client = new TCPClient(config);
        var connRes = client.connect(TCPTestConstants.HOST, TCPTestConstants.PORT);
        connRes.ifErr(err -> {
            throw new RuntimeException("connect failed: " + err);
        });

        var latch = new CountDownLatch(1);

        connRes.ifOk(conn -> new Thread(() -> {
            try {
                // Preallocate buffers
                var sendBuf = ByteBuffer.allocateDirect(PAYLOAD_BYTES);
                var recvBuf = ByteBuffer.allocateDirect(PAYLOAD_BYTES);

                // Warmup
                for (int i = 0; i < WARMUP_MESSAGES; i++) {
                    sendBuf.clear();
                    sendBuf.limit(PAYLOAD_BYTES);
                    // (optional) put some changing bytes; here we just bound the slice
                    writeFully(conn, sendBuf);

                    recvBuf.clear();
                    recvBuf.limit(PAYLOAD_BYTES);
                    readFully(conn, recvBuf);
                }

                // Measure
                long[] rtts = new long[MEASURE_MESSAGES];
                for (int i = 0; i < MEASURE_MESSAGES; i++) {
                    sendBuf.clear();
                    sendBuf.limit(PAYLOAD_BYTES);

                    long t0 = System.nanoTime();
                    writeFully(conn, sendBuf);

                    recvBuf.clear();
                    recvBuf.limit(PAYLOAD_BYTES);
                    readFully(conn, recvBuf);
                    long t1 = System.nanoTime();

                    rtts[i] = t1 - t0;
                }

                // Stats
                Arrays.sort(rtts);
                double p50 = nanosToMicros(rtts[(int) (0.50 * MEASURE_MESSAGES)]);
                double p95 = nanosToMicros(rtts[(int) (0.95 * MEASURE_MESSAGES)]);
                double p99 = nanosToMicros(rtts[(int) (0.99 * MEASURE_MESSAGES)]);
                double avg = Arrays.stream(rtts).average().orElse(0.0) / 1_000.0;

                System.out.printf(
                    "TCP RTT (%dB payload): p50=%.3f µs, p95=%.3f µs, p99=%.3f µs, avg=%.3f µs%n",
                    PAYLOAD_BYTES,
                    p50,
                    p95,
                    p99,
                    avg
                );

                conn.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        }).start());

        latch.await();
        serverBindResult.ifOk(SafeAutoCloseable::close);
    }

    private static void writeFully(com.just.networking.impl.tcp.TCPConnection conn, ByteBuffer buf) throws IOException {
        while (buf.hasRemaining()) {
            int n = conn.transport().write(buf);
            if (n < 0)
                throw new java.nio.channels.ClosedChannelException();
            if (n == 0)
                Thread.onSpinWait();
        }
    }

    private static void readFully(com.just.networking.impl.tcp.TCPConnection conn, ByteBuffer buf) throws IOException {
        while (buf.hasRemaining()) {
            int n = conn.transport().read(buf); // assumes a blocking read(ByteBuffer) exists
            if (n < 0)
                throw new java.nio.channels.ClosedChannelException();
            if (n == 0)
                Thread.onSpinWait();
        }
        buf.flip(); // not strictly needed here unless caller inspects contents
    }

    private static double nanosToMicros(long ns) {
        return ns / 1_000.0;
    }
}
