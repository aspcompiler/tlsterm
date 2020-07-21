package org.amazonli.tlsterm;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.haproxy.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

public class TlsTermProxyFrontendHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(TlsTermProxyFrontendHandler.class);
    private static final byte PP2_TYPE_AWS = (byte)0xEA;
    private static final byte PP2_TYPE_AWS_INTERNAL = (byte)0xEC;
    private static final byte PP2_SUBTYPE_AWS_INTERNAL_CLIENT_CERT_CHAIN = 0x11;
    private static final byte PP2_SUBTYPE_AWS_INTERNAL_VPC_ID = 0x04;
    private static final byte PP2_SUBTYPE_AWS_INTERNAL_PROXY_CONNECTION_ID = 0x70;

    private final String remoteHost;
    private final int remotePort;
    private final boolean sendPPV2;

    // As we use inboundChannel.eventLoop() when building the Bootstrap this does not need to be volatile as
    // the outboundChannel will use the same EventLoop (and therefore Thread) as the inboundChannel.
    private Channel outboundChannel;

    public TlsTermProxyFrontendHandler(String remoteHost, int remotePort, boolean sendPPV2) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.sendPPV2 = sendPPV2;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final Channel inboundChannel = ctx.channel();

        // Start the connection attempt.
        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new TlsTermProxyOutboundInitializer(inboundChannel))
                //.handler(new TlsTermProxyBackendHandler(inboundChannel))
                .option(ChannelOption.AUTO_READ, false);
        ChannelFuture f = b.connect(remoteHost, remotePort);
        outboundChannel = f.channel();
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    // connection complete start to read first data
                    inboundChannel.read();
                } else {
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                }
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        if (outboundChannel.isActive()) {
            outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        // was able to flush out data, start to read the next chunk
                        ctx.channel().read();
                    } else {
                        future.channel().close();
                    }
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        log.info("userEventTriggered: {0}, Class: {1}", evt.toString(), evt.getClass());

        if (evt instanceof SslHandshakeCompletionEvent) {
            if (!sendPPV2) return;

            final Channel inboundChannel = ctx.channel();
            SslHandler sslHandler = (SslHandler)inboundChannel.pipeline().get("ssl");
            try {
                Certificate[] certChain = sslHandler.engine().getSession().getPeerCertificates();
                List<HAProxyTLV> tlvs = new ArrayList<>();
                ByteBuf certContent = Unpooled.buffer();
                certContent.writeByte(PP2_SUBTYPE_AWS_INTERNAL_CLIENT_CERT_CHAIN);
                try(ByteBufOutputStream outputStream = new ByteBufOutputStream(certContent);
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, Charset.defaultCharset())) {
                    JcaPEMWriter writer = new JcaPEMWriter(outputStreamWriter);
                    for (Certificate cert : certChain) {
                        writer.writeObject(cert);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                tlvs.add(new HAProxyTLV(PP2_TYPE_AWS_INTERNAL,certContent));

                InetSocketAddress remoteAddress = (InetSocketAddress)inboundChannel.remoteAddress();
                InetSocketAddress localAddress = (InetSocketAddress)inboundChannel.localAddress();
                String remote = remoteAddress.getAddress().isLoopbackAddress() ? "127.0.0.1" : remoteAddress.getAddress().getHostAddress();
                String local = localAddress.getAddress().isLoopbackAddress() ? "127.0.0.1" : localAddress.getAddress().getHostAddress();
                HAProxyMessage message = new HAProxyMessage(HAProxyProtocolVersion.V2,
                        HAProxyCommand.PROXY,
                        HAProxyProxiedProtocol.TCP4,
                        remote,
                        local,
                        remoteAddress.getPort(),
                        localAddress.getPort(),
                        tlvs
                );
                outboundChannel.writeAndFlush(message).sync();
            } catch (SSLPeerUnverifiedException | InterruptedException uex) {
                log.error("Failed to get the certificate", uex);
            }
        }
    }
    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
