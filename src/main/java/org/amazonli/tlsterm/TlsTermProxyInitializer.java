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

    public TlsTermProxyInitializer(SslContext sslCtx, String remoteHost, int remotePort) {
        this.sslCtx = sslCtx;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(
                new LoggingHandler(LogLevel.INFO),
                sslCtx.newHandler(ch.alloc()),
                new TlsTermProxyFrontendHandler(remoteHost, remotePort));
    }
}
