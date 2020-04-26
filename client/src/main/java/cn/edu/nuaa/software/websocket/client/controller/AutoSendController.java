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
package cn.edu.nuaa.software.websocket.client.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cn.edu.nuaa.software.websocket.client.WsClientApplication;
import cn.edu.nuaa.software.websocket.client.dto.WsNamespaceParameter;
import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: AutoSendController
 * Description:
 * Contact: nuaameteor@qq.com
 * author: Niezhi
 * version: V1.0
 */
@RestController
@Slf4j
@RequestMapping("autoSend")
public class AutoSendController {

    @Autowired
    private SenderController senderController;

    @RequestMapping("/{namespace}/{flag}")
    public String autoStart(@PathVariable("namespace") String namespace, @PathVariable("flag") boolean flag) {
        if (!WsClientApplication.TEST_PARAMETER_CONCURRENT_MAP.containsKey(namespace)) {
            return namespace + " not found";
        }
        WsClientApplication.LOCK_MAP.putIfAbsent(namespace, new ReentrantLock());
        Lock lock = WsClientApplication.LOCK_MAP.get(namespace);
        try {
            if (lock.tryLock(10, TimeUnit.SECONDS)) {
                WsClientApplication.AUTO_SEND_MAP.put(namespace, flag);
                return "OK";
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        return "task is running,try later";
    }

    @Scheduled(cron = "0/10 * * * * ?")
    public void autoSendMsgToWsServer() {
        WsClientApplication.AUTO_SEND_MAP.forEach((k,v)->{
            if (v) {
                WsNamespaceParameter parameter = WsClientApplication.TEST_PARAMETER_CONCURRENT_MAP.get(k);
                log.info("Auto start task {}", parameter.getNamespace());
                senderController.sendMessage(parameter.getNamespace(), parameter.getThreadCount(), parameter.getTaskCountPerThread());
            }
        });
    }

}
