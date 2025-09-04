package com.just.networking.config;

import com.bvanseg.just.functional.option.Option;

import java.nio.ByteOrder;

public class DefaultConfigKeys {

    private static final ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.BIG_ENDIAN;

    // TCP Client Socket
    public static final ConfigKey.Default<Boolean> TCP_CLIENT_SOCKET_BLOCKING = new ConfigKey.Default<>(
        "tcp.client.socket.blocking",
        true
    );

    public static final ConfigKey.Default<Integer> TCP_CLIENT_SOCKET_CONNECT_TIMEOUT_MILLIS = new ConfigKey.Default<>(
        "tcp.client.socket.connect_timeout_millis",
        0
    );

    public static final ConfigKey.Default<Boolean> TCP_CLIENT_SOCKET_KEEP_ALIVE = new ConfigKey.Default<>(
        "tcp.client.socket.keep_alive",
        true
    );

    public static final ConfigKey.Default<Boolean> TCP_CLIENT_SOCKET_NO_DELAY = new ConfigKey.Default<>(
        "tcp.client.socket.no_delay",
        true
    );

    public static final ConfigKey.Default<Option<Integer>> TCP_CLIENT_SOCKET_SO_LINGER_SECONDS =
        new ConfigKey.Default<>("tcp.client.socket.so_linger_seconds", Option.none());

    // TCP Client Byte Read Loop

    public static final ConfigKey.Default<Integer> TCP_READ_LOOP_BUFFER_SIZE_IN_BYTES =
        new ConfigKey.Default<>("tcp.read_loop.buffer_size_in_bytes", 64 * 1024);

    public static final ConfigKey.Default<ByteOrder> TCP_READ_LOOP_BYTE_ORDER =
        new ConfigKey.Default<>("tcp.read_loop.byte_order", DEFAULT_BYTE_ORDER);

    // TCP Frame Read Channel

    public static final ConfigKey.Default<Integer> TCP_FRAME_READ_CHANNEL_BUFFER_SIZE_IN_BYTES =
        new ConfigKey.Default<>(
            "tcp.frame.read_channel.buffer_size_in_bytes",
            256 * 1024
        );

    public static final ConfigKey.Default<ByteOrder> TCP_FRAME_READ_CHANNEL_BYTE_ORDER = new ConfigKey.Default<>(
        "tcp.frame.read_channel.byte_order",
        DEFAULT_BYTE_ORDER
    );

    // TCP Frame Write Channel

    public static final ConfigKey.Default<Integer> TCP_FRAME_WRITE_CHANNEL_BUFFER_SIZE_IN_BYTES =
        new ConfigKey.Default<>(
            "tcp.frame.write_channel.buffer_size_in_bytes",
            4 * 1024 * 1024
        );

    public static final ConfigKey.Default<ByteOrder> TCP_FRAME_WRITE_CHANNEL_BYTE_ORDER = new ConfigKey.Default<>(
        "tcp.frame.write_channel.byte_order",
        DEFAULT_BYTE_ORDER
    );

    // TCP Message Write Channel

    public static final ConfigKey.Default<Integer> TCP_MESSAGE_WRITE_CHANNEL_SCRATCH_BUFFER_SIZE_IN_BYTES =
        new ConfigKey.Default<>(
            "tcp.message.write_channel.scratch_buffer_size_in_bytes",
            8 * 1024
        );

    public static final ConfigKey.Default<Integer> TCP_MESSAGE_WRITE_CHANNEL_OUT_BUFFER_SIZE_IN_BYTES =
        new ConfigKey.Default<>(
            "tcp.message.write_channel.out_buffer_size_in_bytes",
            64 * 1024
        );

    public static final ConfigKey.Default<ByteOrder> TCP_MESSAGE_WRITE_CHANNEL_BYTE_ORDER = new ConfigKey.Default<>(
        "tcp.message.write_channel.byte_order",
        DEFAULT_BYTE_ORDER
    );

    // TCP Server Socket
    public static final ConfigKey.Default<Boolean> TCP_SERVER_SOCKET_BLOCKING = new ConfigKey.Default<>(
        "tcp.server.socket.blocking",
        true
    );

    public static final ConfigKey.Default<Boolean> TCP_SERVER_SOCKET_SO_REUSEADDR =
        new ConfigKey.Default<>("tcp.server.socket.so_reuseaddr", true);

    // TCP Server Client Socket

    public static final ConfigKey.Default<Boolean> TCP_SERVER_CLIENT_SOCKET_BLOCKING = new ConfigKey.Default<>(
        "tcp.server.client.socket.blocking",
        true
    );

    public static final ConfigKey.Default<Boolean> TCP_SERVER_CLIENT_SOCKET_KEEP_ALIVE = new ConfigKey.Default<>(
        "tcp.server.client.socket.keep_alive",
        true
    );

    public static final ConfigKey.Default<Boolean> TCP_SERVER_CLIENT_SOCKET_NO_DELAY = new ConfigKey.Default<>(
        "tcp.server.client.socket.no_delay",
        true
    );
}
