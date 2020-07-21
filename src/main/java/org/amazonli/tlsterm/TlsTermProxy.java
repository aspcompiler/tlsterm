package org.amazonli.tlsterm;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TlsTermProxy {
    static final int LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "8883"));
    static final String REMOTE_HOST = System.getProperty("remoteHost", "localhost");
    static final int REMOTE_PORT = Integer.parseInt(System.getProperty("remotePort", "10183"));
    static final boolean SEND_PPV2 = Boolean.parseBoolean(System.getProperty("ppv2", "true"));

    static final Logger log = LoggerFactory.getLogger(TlsTermProxy.class);


    public static void main(String[] args) throws Exception {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);


        //System.out.println("Proxying *:" + LOCAL_PORT + " to " + REMOTE_HOST + ':' + REMOTE_PORT + " ...");
        log.info("Proxying *:" + LOCAL_PORT + " to " + REMOTE_HOST + ':' + REMOTE_PORT + " ...");

        //TLS
        //SelfSignedCertificate ssc = new SelfSignedCertificate();
        //final SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

        final SslContext sslCtx = SslContextBuilder.forServer(
                new File("/Users/amazonli/certs/server.crt"),
                new File("/Users/amazonli/certs/server.pem"))
                .clientAuth(ClientAuth.REQUIRE)
                .trustManager(TlsTermTrustManagerFactory.INSTANCE)
                .build();

        // Configure the bootstrap.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new TlsTermProxyInitializer(sslCtx, REMOTE_HOST, REMOTE_PORT, SEND_PPV2))
                    .childOption(ChannelOption.AUTO_READ, false)
                    .bind(LOCAL_PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
