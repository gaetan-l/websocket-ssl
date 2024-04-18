package com.gaetanl.websocket.client;

import java.io.*;
import java.net.URI;

import org.slf4j.*;

import com.gaetanl.websocket.message.*;
import com.gaetanl.websocket.util.*;
import com.gaetanl.websocket.util.WebSocketUtil;
import com.google.gson.*;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;

public final class WebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClient.class);

    private enum WebSocketProtocol {WS, WSS};

    public static void main(String[] args) throws Exception {
        final String protocol;
        final String host;
        final int port;
        try {
            protocol = WebSocketProtocol.valueOf(args[0]).toString().toLowerCase();
            host = args[1];
            port = Integer.valueOf(args[2]);
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

            throw new IllegalArgumentException("Excepted args: WS|WSS host port, got: " + argsString.toString());
        }

        final URI uri = new URI(String.format("%s://%s:%d%s", protocol, host, port, WebSocketUtil.WEBSOCKET_PATH));
        final SslContext sslCtx = ClientUtil.buildSslContext("wss".equalsIgnoreCase(protocol));

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
            // If you change it to V00, ping is not supported and remember to change
            // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
            final WebSocketClientHandler handler = new WebSocketClientHandler(
                    WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()));

            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ChannelPipeline p = ch.pipeline();

                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                     }
                     p.addLast(
                             new HttpClientCodec(),
                             new HttpObjectAggregator(8192),
                             WebSocketClientCompressionHandler.INSTANCE,
                             handler,
                             new OutboundLoggingHandler());
                 }
             });

            logger.info(String.format("Trying to create channel with %s://%s:%d", protocol, uri.getHost(), port));
            Channel ch = b.connect(uri.getHost(), port).sync().channel();
            handler.handshakeFuture().sync();
            logger.info("[OK] Channel created, enter commands:");

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            while (ch.isActive()) {
                String input = console.readLine();
                if (input == null) {
                    break;
                }

                WsMessage message = null;

                try {
                    if ("ping".equals(input)) {
                        WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[] { 8, 1, 8, 1 }));
                        ch.writeAndFlush(frame);
                    }
                    else if ("deconnection".equals(input)) {
                        message = new WsMsgDeconnexion();
                        logger.debug("Sending disconnection message to server...");
                    }
                    else {
                        message = new WsMsgText();
                        ((WsMsgText) message).setText(input);
                    }
                }
                catch (Exception e) {
                    logger.error("Error during websocket server parsing loop", e.getMessage(), e);
                    e.printStackTrace();
                }

                if (message != null) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    String json = gson.toJson(message);

                    WebSocketFrame frame = new TextWebSocketFrame(json);
                    ch.writeAndFlush(frame);

                    if (message instanceof WsMsgDeconnexion) {
                        logger.debug("Closing channel...");
                        ChannelFuture futureClose = ch.closeFuture().sync();

                        futureClose.addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture arg0) throws Exception {
                                logger.debug("[OK] Channel closed");
                            }
                        });
                    }
                }
            }
        } finally {
            group.shutdownGracefully();
        }
    }
}