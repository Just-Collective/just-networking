package com.just.networking.tcp.latency;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import com.just.networking.SafeAutoCloseable;
import com.just.networking.config.frame.TCPFrameConfig;
import com.just.networking.impl.frame.client.TCPFrameClient;
import com.just.networking.impl.frame.server.TCPFrameServer;
import com.just.networking.tcp.TCPTestConstants;

public class TCPFrameLatencyTest {

    // ---- Tuning knobs ----
    static final int PAYLOAD_BYTES = 32;

    static final int WARMUP_MESSAGES = 5_000;

    static final int MEASURE_MESSAGES = 50_000;

    public static void main(String[] args) throws Exception {
        var server = new TCPFrameServer();
        var serverBindResult = server.bind(TCPTestConstants.HOST, TCPTestConstants.PORT);

        serverBindResult.ifOk(tcpFrameServerConnection -> {
            // Echo server: send back each received frame; flush so client sees it promptly.
            tcpFrameServerConnection.listen(() -> (connection, payload) -> {
                try {
                    // payload is already in read mode with remaining() == frame size.
                    // sendFrame copies; we can pass payload directly.
                    connection.transport().sendFrame(payload);
                    connection.transport().flushWrites();
                } catch (IOException ignore) {
                    // test-only
                }
            });
        });

        var client = new TCPFrameClient(TCPFrameConfig.DEFAULT);
        var connRes = client.connect(TCPTestConstants.HOST, TCPTestConstants.PORT);
        connRes.ifErr(err -> {
            throw new RuntimeException("connect failed: " + err);
        });

        var done = new CountDownLatch(1);

        connRes.ifOk(conn -> new Thread(() -> {
            try {
                var sendBuf = ByteBuffer.allocateDirect(PAYLOAD_BYTES);
                long[] rtts = new long[MEASURE_MESSAGES];

                // Warmup
                for (int i = 0; i < WARMUP_MESSAGES; i++) {
                    sendBuf.clear();
                    sendBuf.limit(PAYLOAD_BYTES);
                    conn.transport().sendFrame(sendBuf);
                    conn.transport().flushWrites();

                    // Block until we get the echoed frame
                    ByteBuffer echo;
                    while ((echo = conn.transport().readFrame()) == null) {
                        /* spin */ }
                    // Optionally: validate echo.remaining() == PAYLOAD_BYTES
                }

                // Measure
                for (int i = 0; i < MEASURE_MESSAGES; i++) {
                    sendBuf.clear();
                    sendBuf.limit(PAYLOAD_BYTES);

                    long t0 = System.nanoTime();
                    conn.transport().sendFrame(sendBuf);
                    conn.transport().flushWrites();

                    ByteBuffer echo;
                    while ((echo = conn.transport().readFrame()) == null) {
                        /* spin */ }
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
                    "TCP Frames RTT (%dB payload): p50=%.3f µs, p95=%.3f µs, p99=%.3f µs, avg=%.3f µs%n",
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
                done.countDown();
            }
        }).start());

        done.await();
        serverBindResult.ifOk(SafeAutoCloseable::close);
    }

    private static double nanosToMicros(long ns) {
        return ns / 1_000.0;
    }
}
