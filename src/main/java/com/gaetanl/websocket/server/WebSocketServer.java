package com.gaetanl.websocket.server;

import java.util.*;

import org.slf4j.*;

import com.gaetanl.websocket.util.*;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.*;
import io.netty.handler.ssl.SslContext;


public final class WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    private enum WebSocketProtocol {WS, WSS};
    private static final List<Channel> openChannels = new ArrayList<Channel>();

    public static void main(String[] args) throws Exception {
        final String protocol;
        final int port;

        logger.info("Initiating server configuration");
        try {
            protocol = WebSocketProtocol.valueOf(args[0]).toString().toLowerCase();
            port = Integer.valueOf(args[1]);
        }
        catch (Exception e) {
            logger.error("Exception parsing main arguments", e);

            StringBuilder argsString = new StringBuilder().append("[");
            for (int i = 0 ; i < args.length ; i++) {
                argsString.append(args[i]);
                if (i < args.length - 1) {
                    argsString.append(" ");
                }
            }
            argsString.append("]");

            throw new IllegalArgumentException("Excepted args: WS|WSS port [HTTPServerHandler class name], got: " + argsString.toString());
        }

        final SslContext sslCtx = ServerUtil.buildSslContext("wss".equalsIgnoreCase(protocol));

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    if (sslCtx != null) {
                        System.out.println("Adding SSL handler");
                        pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                    }
                    pipeline.addLast(new HttpServerCodec());
                    pipeline.addLast(new HttpObjectAggregator(65536));
                    //pipeline.addLast(new WebSocketServerHttpHandler());
                    pipeline.addLast(new WebSocketServerCompressionHandler());
                    pipeline.addLast(new WebSocketServerProtocolHandler(WebSocketUtil.WEBSOCKET_PATH, null, true));
                    pipeline.addLast(new WebSocketServerFrameHandler());

                    System.out.println("Adding logging handler");
                    pipeline.addLast(new OutboundLoggingHandler());
                }
             });

            Channel ch = b.bind(port).sync().channel();
            logger.info(String.format("[OK] Server started at %s://%s:%d", protocol, "localhost", port));
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}