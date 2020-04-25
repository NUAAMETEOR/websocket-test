/**
 * Copyright 2009-2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.edu.nuaa.software.websocket.client.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import cn.edu.nuaa.software.websocket.client.WsClientApplication;
import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: Client
 * Description:
 * Contact: nuaameteor@qq.com
 * author: Niezhi
 * version: V1.0
 */
@Slf4j
public class Receiver extends WebSocketClient {

    private final String name;
    private final String namespace;
    private       final String           url;

    public Receiver(String namespace, String name, String url) {
        super(URI.create(url));
        this.url=url;
        this.name=name;
        this.namespace = namespace;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("client {} 连接成功", name);
        AtomicInteger defaultValue = new AtomicInteger(0);
        WsClientApplication.COUNT_MAP.putIfAbsent(namespace, defaultValue);
        WsClientApplication.COUNT_MAP.get(namespace).incrementAndGet();
    }

    @Override
    public void onMessage(String message) {
        Map parse = null;
        try {
            parse = JSONObject.parseObject(message, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("client {} error {}", name, e.getMessage());
            return;
        }
        if (parse != null && parse.containsKey("protocalId") && "PROTO_HEART_BEAT".equals(parse.get("protocolId").toString())) {
            Map map = new HashMap();
            map.put("path", "internal/heart_beat");
            map.put("protocolId", "PROTO_HEART_BEAT");
            map.put("sourceType", "request");
            map.put("taskId", UUID.randomUUID().toString());
            String text = JSON.toJSONString(map);
            send(text);
            log.debug("send heart beat {}", text);
        } else {
            log.debug("client {} receive message [{}]", name, message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info(" client {} quit", name);
        WsClientApplication.COUNT_MAP.get(namespace).decrementAndGet();
    }

    @Override
    public void onError(Exception ex) {
        log.info(" client {} exception", ex.getMessage());
    }
}
