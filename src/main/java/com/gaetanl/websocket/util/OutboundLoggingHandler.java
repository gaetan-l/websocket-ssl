package com.gaetanl.websocket.util;

import static com.gaetanl.websocket.util.WebSocketUtil.Direction.OUTBOUND;

import org.slf4j.*;

import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class OutboundLoggingHandler extends ChannelOutboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(OutboundLoggingHandler.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        logger.debug(WebSocketUtil.getMessageDetails(OUTBOUND, msg, ctx.channel()));

        String type = msg.getClass().getSimpleName();
        if (msg instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) msg).text().replace("\n", "\n    ");
            logger.info(String.format("[OK] websocket/out: TextWebSocketFrame (see below)\n\n    %s\n", text));
        }
        else {
            logger.info(String.format("[OK] websocket/out: %s", type));
        }

        // Delegate the actual writing to the next handler in the pipeline
        super.write(ctx, msg, promise);
    }
}