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
package com.gaetanl.websocket.server;

import static com.gaetanl.websocket.util.WebSocketUtil.Direction.INBOUND;

import org.slf4j.*;

import com.gaetanl.websocket.message.*;
import com.gaetanl.websocket.util.WebSocketUtil;
import com.google.gson.*;

import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.*;

/**
 * Echoes uppercase content of text frames.
 */
public class WebSocketServerFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServerFrameHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        logger.debug(WebSocketUtil.getMessageDetails(INBOUND, frame, ctx.channel()));

        // Ping and pong frames already handled

        // By default only the class name of the frame is logged
        String logMessageRecu = frame.getClass().getSimpleName();

        // Indicating if there is a treatment for the received frame
        boolean traitement = false;

        // Acknowledgement to be returned by the server
        WebSocketFrame responseFrame = null;

        if (frame instanceof TextWebSocketFrame) {
            String receivedText = ((TextWebSocketFrame) frame).text();
            logMessageRecu = receivedText.replaceAll("(?m)^", "    ");

            WsMessage receivedMessage = null;
            try {
                GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
                builder.registerTypeAdapter(WsMessage.class, new WsMessageDeserializer());
                Gson gson = builder.create();
                receivedMessage = gson.fromJson(receivedText, WsMessage.class);

                String receivedType = receivedMessage.getType();
                if (WsMsgText.class.getName().equals(receivedType)) {
                    // Do nothing
                }
                else if (WsMsgDeconnexion.class.getName().equals(receivedType)) {
                    ctx.channel().close();
                    // TODO: Code métier
                }
                else if (WsAckText.class.getName().equals(receivedType)) {
                    // Do nothing
                }

                WsMessage ack = receivedMessage.getAck();

                if (ack != null) {
                    Gson gsonWriter = new GsonBuilder().setPrettyPrinting().create();
                    responseFrame = new TextWebSocketFrame(gsonWriter.toJson(ack));
                }
            }
            catch (JsonSyntaxException e) {
                logger.info(String.format("[NOK] websocket/in:  TextWebSocketFrame/WsMessage ('%s' couldn't be parsed)", receivedText), e.getMessage(), e);
            }
        }
        else if (frame instanceof PingWebSocketFrame) {
            responseFrame = new PongWebSocketFrame();
        }
        else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }

        logger.info(String.format("[OK] websocket/in:\n\n%s\n", logMessageRecu));

        // Logging if no treatment done
        if (!traitement) {
            logger.debug(String.format("No action defined for type %s", frame.getClass().getSimpleName()));
        }

        // Returning ack if it exists
        if (responseFrame != null) {
            ctx.channel().writeAndFlush(responseFrame);
        }
    }

//    @Override
//    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
//            //Channel upgrade to websocket, remove WebSocketIndexPageHandler.
//            ctx.pipeline().remove(WebSocketServerHttpHandler.class);
//        } else {
//            super.userEventTriggered(ctx, evt);
//        }
//    }
}