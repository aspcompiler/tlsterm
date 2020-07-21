package org.amazonli.tlsterm;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class TlsTermProxyOutboundInitializer extends ChannelInitializer<SocketChannel> {

    private Channel inboundChannel;

    public TlsTermProxyOutboundInitializer(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(
            new HAProxyHandler(),
            new TlsTermProxyBackendHandler(inboundChannel));
    }
}
