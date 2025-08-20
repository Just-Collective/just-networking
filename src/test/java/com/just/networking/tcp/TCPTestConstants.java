package com.just.networking.tcp;

public class TCPTestConstants {

    public static final String HOST = "localhost";

    public static final int PORT = 4000;

    public static final long TOTAL_MEGABYTES = 10_000L;

    public static final int MB_SIZE = 1024 * 1024;

    public static final long TOTAL_BYTES = TOTAL_MEGABYTES * MB_SIZE;

    public static final int CHUNK_SIZE = MB_SIZE;
}
