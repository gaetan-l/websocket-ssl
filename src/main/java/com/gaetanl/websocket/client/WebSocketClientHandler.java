/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
//The MIT License
//
//Copyright (c) 2009 Carl Bystršm
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//THE SOFTWARE.

package com.gaetanl.websocket.client;

import static com.gaetanl.websocket.message.WsMessageConstants.*;
import static com.gaetanl.websocket.util.WebSocketUtil.Direction.INBOUND;

import org.slf4j.*;

import com.gaetanl.websocket.message.*;
import com.gaetanl.websocket.util.WebSocketUtil;
import com.google.gson.*;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);

    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("[OK] websocket client disconnected");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug(WebSocketUtil.getMessageDetails(INBOUND, msg, ctx.channel()));

        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                logger.info("[OK] Handshake done");
                handshakeFuture.setSuccess();
            }
            catch (WebSocketHandshakeException e) {
                logger.error("[NOK] Handshake failed", e.getMessage(), e);
                handshakeFuture.setFailure(e);
            }
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof WebSocketFrame) {

            if (frame instanceof BinaryWebSocketFrame) {
                logger.info("[OK] websocket/in: BinaryWebSocketFrame");
            }

            else if (frame instanceof TextWebSocketFrame) {
                String text = ((TextWebSocketFrame) frame).text();
                logger.info(String.format("[OK] websocket/in: TextWebSocketFrame (see below)\n\n%s\n", text.replaceAll("(?m)^", "    ")));

                WsMessage message = null;
                try {
                    GsonBuilder builder = new GsonBuilder();
                    builder.registerTypeAdapter(WsMessage.class, new WsMessageDeserializer());
                    Gson gsonReader = builder.create();
                    message = gsonReader.fromJson(text, WsMessage.class);

                    String type = message.getType();
                    if (WS_MSG_TEXT.equals(type)) {
                        // Do nothing
                    }
                    else if (WS_ACK_TEXT.equals(type)) {
                        // Do nothing
                    }

                    if (message != null) {
                        WsMessage ack = message.getAck();

                        if (ack != null) {
                            Gson gsonWriter = new GsonBuilder().setPrettyPrinting().create();
                            String json = gsonWriter.toJson(ack);
                            ctx.channel().writeAndFlush(new TextWebSocketFrame(json));
                        }
                    }
                }
                catch (JsonParseException e) {
                    logger.info(String.format("[NOK] websocket/in: WebSocketFrame/WsMessage ('%s' could'nt be parsed)", text), e.getMessage(), e);
                }
            }

            else if (frame instanceof PingWebSocketFrame) {
                logger.info("[OK] websocket/in: PingWebSocketFrame");
            }

            else if (frame instanceof PongWebSocketFrame) {
                logger.info("[OK] websocket/in: PongWebSocketFrame");
            }

            else if (frame instanceof CloseWebSocketFrame) {
                final String reasonText = ((CloseWebSocketFrame) frame).reasonText();
                final int statusCode = ((CloseWebSocketFrame) frame).statusCode();
                logger.info(String.format("[OK] websocket/in: CloseWebSocketFrame (reasonText=%s, statusCode=%d)", reasonText, statusCode));
                ch.close();
            }

            else {
                logger.info("[NOK] websocket/in: WebSocketFrame (couldn't be parsed)");
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }
}