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
package cn.edu.nuaa.software.websocket.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import cn.edu.nuaa.software.websocket.client.controller.SenderController;
import cn.edu.nuaa.software.websocket.client.dto.WsTestParameter;
import cn.edu.nuaa.software.websocket.client.netty.Client;
import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: WsClientApplication
 * Description:
 * Contact: nuaameteor@qq.com
 * author: Niezhi
 * version: V1.0
 */
@SpringBootApplication
@Slf4j
@RestController
public class WsClientApplication implements ApplicationListener<ApplicationReadyEvent> {

    public static final ConcurrentMap<String, WsTestParameter> TEST_PARAMETER_CONCURRENT_MAP = new ConcurrentHashMap<>(16);
    public static final ConcurrentMap<String, Queue<Client>>   CLIENT_MAP                    = new ConcurrentHashMap<>(16);
    public static final ConcurrentMap<String, Boolean>         STATUS_MAP                    = new ConcurrentHashMap<>(16);
    public static final ConcurrentMap<String, Boolean>         AUTO_SEND_MAP                 = new ConcurrentHashMap<>(16);
    public static final ConcurrentMap<String, Lock>            LOCK_MAP                      = new ConcurrentHashMap<>(16);
    public static final ConcurrentMap<String, AtomicInteger>   COUNT_MAP                     = new ConcurrentHashMap<>(16);
    @Autowired
    private SenderController senderController;

    public static String url = "ws://localhost:9000";

    public static void main(String[] args) {
        SpringApplication.run(WsClientApplication.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String s = System.getProperty("url", null);
        if (StringUtils.hasText(s)) {
            url = s;
        }
        log.info("use websocket address:{}", url);
        s = System.getProperty("auto");
        if (s != null) {
            String namespace = System.getProperty("n");
            int threadCount = Integer.valueOf(System.getProperty("t"));
            int taskCountPerThread = Integer.valueOf(System.getProperty("c"));
            senderController.sendMessage(namespace, threadCount, taskCountPerThread);
            AUTO_SEND_MAP.put(namespace, true);
        }
    }
}

