package com.gaetanl.websocket.util;

import java.io.*;
import java.net.InetSocketAddress;

import io.netty.channel.*;
import io.netty.handler.ssl.SslHandler;

public class WebSocketUtil {
    public static final String WEBSOCKET_PATH = "/websocket";

    public static InetSocketAddress getRemoteAddress(ChannelHandlerContext ctx) {
        return getRemoteAddress(ctx.channel());
    }

    public static InetSocketAddress getRemoteAddress(Channel channel) {
        return ((InetSocketAddress) channel.remoteAddress());
    }

    public static String asString(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        // Create a StringBuilder to store the content read from the input
        // stream
        StringBuilder stringBuilder = new StringBuilder();

        // Create a BufferedReader to read the content from the input stream
        InputStreamReader inputStreamReader;
        BufferedReader bufferedReader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        }
        finally {
            // Close the BufferedReader
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                }
                catch (IOException e) {
                    // Handle or log the exception
                }
            }
        }

        // Convert the StringBuilder to a String and return it
        return stringBuilder.toString();
    }

    public static enum Direction {INBOUND, OUTBOUND};
    public static String getMessageDetails(final Direction direction, final Object message, final Channel channel) {
        final String channelDetails = channel.toString();
        boolean isSecure = channel.pipeline().get(SslHandler.class) != null;
        return String.format("%s (%s) message on channel (%s) %s",
                direction, message.getClass().getSimpleName(), isSecure ? "SECURE" : "NOT SECURE", channelDetails);
    }
}
