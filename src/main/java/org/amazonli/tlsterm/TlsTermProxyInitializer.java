package org.amazonli.tlsterm;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

public class TlsTermProxyInitializer extends ChannelInitializer<SocketChannel> {

    private  SslContext sslCtx;
    private final String remoteHost;
    private final int remotePort;
    private final boolean sendPPV2;

    public TlsTermProxyInitializer(SslContext sslCtx, String remoteHost, int remotePort, boolean sendPPV2) {
        this.sslCtx = sslCtx;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.sendPPV2 = sendPPV2;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
        ch.pipeline().addLast("ssl", sslCtx.newHandler(ch.alloc()));
        ch.pipeline().addLast(new TlsTermProxyFrontendHandler(remoteHost, remotePort, sendPPV2));
    }
}
