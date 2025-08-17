package com.just.networking.impl.frame.client;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.just.networking.ChannelIO;
import com.just.networking.ConnectionBroker;
import com.just.networking.impl.frame.TCPFrameChannel;
import com.just.networking.impl.tcp.client.TCPClient;

public final class TCPFrameClient implements AutoCloseable {

    private final TCPClient tcpClient;

    private final TCPFrameChannel tcpFrameChannel;

    public TCPFrameClient() {
        this(new TCPClient());
    }

    public TCPFrameClient(TCPClient tcpClient) {
        this.tcpClient = tcpClient;
        this.tcpFrameChannel = new TCPFrameChannel(new ChannelIO() {

            @Override
            public int read(ByteBuffer dst) throws IOException {
                return tcpClient.read(dst);
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                return tcpClient.write(src);
            }
        });
    }

    @Override
    public void close() {
        disconnect();
    }

    public ConnectionBroker.ConnectResult connect(String host, int port) {
        return tcpClient.connect(host, port);
    }

    public ByteBuffer readFrame() throws IOException {
        return tcpFrameChannel.readFrame();
    }

    public void sendFrame(ByteBuffer payload) throws IOException {
        tcpFrameChannel.sendFrame(payload);
    }

    public void flushWrites() throws IOException {
        tcpFrameChannel.flushWrites();
    }

    public void disconnect() {
        tcpClient.disconnect();
        tcpFrameChannel.close();
    }

    public boolean isOpen() {
        return tcpClient.isOpen();
    }
}
