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

import javax.net.ssl.TrustManagerFactory;

import com.gaetanl.websocket.client.WebSocketClient;

import io.netty.handler.ssl.*;

/**
 * Some useful methods for server side.
 */
public final class ClientUtil {
    private ClientUtil() {
    }

    public static SslContext buildSslContext(final boolean ssl)
            throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException {
        if (!ssl) {
            return null;
        }
        String path = "/TestTruststore.jks";
        System.out.println(String.format("trustore content=\"%s\"", WebSocketUtil.asString(WebSocketClient.class.getResourceAsStream(path))));
        KeyStore truststore = KeyStore.getInstance("JKS");
        InputStream is = ClientUtil.class.getResourceAsStream(path);
        truststore.load(is, "testtest".toCharArray());
        System.out.println("truststore=" + truststore);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(truststore);
        return SslContextBuilder.forClient().trustManager(trustManagerFactory).build();
    }
}