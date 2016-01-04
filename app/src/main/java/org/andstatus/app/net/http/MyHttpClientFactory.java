/*
 * Copyright (C) 2014 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app.net.http;

import org.andstatus.app.context.MyPreferences;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class MyHttpClientFactory {

    /** Based on: https://github.com/rfc2822/davdroid/blob/master/src/at/bitfire/davdroid/webdav/DavHttpClient.java */

    private MyHttpClientFactory() {
        // Empty
    }
    
    public static HttpClient getHttpClient(SslModeEnum sslMode) {
        Registry<ConnectionSocketFactory> registry = 
                RegistryBuilder.<ConnectionSocketFactory> create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", TlsSniSocketFactory.getInstance(sslMode))
                    .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        // max.  3 connections in total
        connectionManager.setMaxTotal(3);
        // max.  2 connections per host
        connectionManager.setDefaultMaxPerRoute(2);

        // use request defaults from AndroidHttpClient
        RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
                .setConnectTimeout(MyPreferences.getConnectionTimeoutMs())
                .setSocketTimeout(2*MyPreferences.getConnectionTimeoutMs())
                .setStaleConnectionCheckEnabled(false)
                .build();
        
        HttpClientBuilder builder = HttpClients.custom()
                .useSystemProperties()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                /* TODO maybe:  
                .setRetryHandler(DavHttpRequestRetryHandler.INSTANCE)
                .setRedirectStrategy(DavRedirectStrategy.INSTANCE)  
                */
                .disableRedirectHandling()
                .setUserAgent(HttpConnection.USER_AGENT)
                .disableCookieManagement();

        return builder.build();
    }
    
}
