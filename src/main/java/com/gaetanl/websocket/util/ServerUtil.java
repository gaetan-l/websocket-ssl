/*
 * Copyright 2022 The Netty Project
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
package com.gaetanl.websocket.util;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;

import javax.net.ssl.*;

import com.gaetanl.websocket.server.WebSocketServer;

import io.netty.handler.ssl.*;

/**
 * Some useful methods for server side.
 */
public final class ServerUtil {
    private ServerUtil() {
    }

    public static SslContext buildSslContext(final boolean ssl)
            throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        if (!ssl) {
            return null;
        }
        String path = "/TestKeystore.jks";
        System.out.println(String.format("keystore content=\"%s\"", WebSocketUtil.asString(WebSocketServer.class.getResourceAsStream(path))));
        KeyStore keystore = KeyStore.getInstance("JKS");
        InputStream is = ServerUtil.class.getResourceAsStream(path);
        keystore.load(is, "testtest".toCharArray());
        System.out.println("keystore=" + is);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, "testtest".toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return SslContextBuilder.forServer(keyManagerFactory).build();
    }
}