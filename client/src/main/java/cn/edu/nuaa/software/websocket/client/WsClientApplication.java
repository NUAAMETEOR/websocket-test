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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import cn.edu.nuaa.software.websocket.client.controller.SenderController;
import cn.edu.nuaa.software.websocket.client.dto.AuditPojo;
import cn.edu.nuaa.software.websocket.client.dto.ReportDto;
import cn.edu.nuaa.software.websocket.client.dto.WsClientTestParameteor;
import cn.edu.nuaa.software.websocket.client.dto.WsNamespaceParameter;
import cn.edu.nuaa.software.websocket.client.netty.Receiver;
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
@EnableScheduling
@RestController
@EnableConfigurationProperties(WsClientTestParameteor.class)
public class WsClientApplication implements ApplicationListener<ApplicationReadyEvent> {

    public static final ConcurrentMap<String, WsNamespaceParameter> TEST_PARAMETER_CONCURRENT_MAP = new ConcurrentHashMap<>(16);
    public static final ConcurrentMap<String, Queue<Receiver>>      CLIENT_MAP                    = new ConcurrentHashMap<>(16);
    public static final ConcurrentMap<String, Boolean>         STATUS_MAP                    = new ConcurrentHashMap<>(16);
    public static final ConcurrentMap<String, Boolean>         AUTO_SEND_MAP                 = new ConcurrentHashMap<>(16);
    public static final ConcurrentMap<String, Lock>            LOCK_MAP                      = new ConcurrentHashMap<>(16);
    public static final ConcurrentMap<String, AtomicInteger>   CONNECTION_COUNT_MAP          = new ConcurrentHashMap<>(16);
    public static final ConcurrentMap<String, AuditPojo> AUDIT_MAP = new ConcurrentHashMap<>(16);
    public static final ConcurrentMap<String, ReportDto> REPORTS_MAP = new ConcurrentHashMap<>(16);
    public static String                                 url       = "ws://localhost:9000";

    @Autowired
    private WsClientTestParameteor clientTestParameteor;

    @Autowired
    private             SenderController                       senderController;

    public static void main(String[] args) {
        SpringApplication.run(WsClientApplication.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("use websocket address:{}", clientTestParameteor.getWsServerUrl());
        String s = System.getProperty("auto");
        if (s != null) {
            String namespace          = System.getProperty("n");
            int    threadCount        = Integer.valueOf(System.getProperty("t"));
            int    taskCountPerThread = Integer.valueOf(System.getProperty("c"));
            AUTO_SEND_MAP.put(namespace, true);
            senderController.sendMessage(namespace, threadCount, taskCountPerThread);
        }
    }
}

